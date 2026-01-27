package umm3601.todo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
// import io.javalin.json.JavalinJackson;

public class TodoControllerSpec {

  private TodoController todoController;

  private ObjectId specialTodoId;

  // The client and database that will be used
  // for all the tests in this spec file.
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  // Used to translate between JSON and POJOs. Used for testing add methods.
  // private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() throws IOException {
    // Reset our mock context and argument captor (declared with Mockito annotations
    // @Mock and @Captor)
    MockitoAnnotations.openMocks(this);

    // Setup database
    MongoCollection<Document> todoDocuments = db.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();
    testTodos.add(
      new Document()
      .append("owner", "Kristin")
      .append("status", true)
      .append("body", "Buy soup and ice cream")
      .append("category", "groceries")
    );
    testTodos.add(
      new Document()
      .append("owner", "Kristin")
      .append("status", false)
      .append("body", "Buy cereal and soup")
      .append("category", "groceries")
    );
    testTodos.add(
      new Document()
      .append("owner", "Nic")
      .append("status", false)
      .append("body", "Write an essay about soup")
      .append("category", "homework")
    );

    specialTodoId = new ObjectId();
    Document specialTodo = new Document()
      .append("_id", specialTodoId)
      .append("owner", "Elena")
      .append("status", false)
      .append("body", "Write an essay about walking on ice")
      .append("category", "homework");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(specialTodo);

    todoController = new TodoController(db);
  }

  @Test
  void addsRoutes() {
    Javalin mockServer = mock(Javalin.class);
    todoController.addRoutes(mockServer);
    verify(mockServer, Mockito.atLeast(1)).get(any(), any());
    // verify(mockServer, Mockito.atLeastOnce()).post(any(), any());
    // verify(mockServer, Mockito.atLeastOnce()).delete(any(), any());
  }


  @Test
  void canGetAllTodos() throws IOException {
    // When something asks the (mocked) context for the queryParamMap,
    // it will return an empty map (since there are no query params in
    // this case where we want all todos).
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    // Now, go ahead and ask the todoController to getTodos
    // (which will, indeed, ask the context for its queryParamMap)
    todoController.getTodos(ctx);

    // We are going to capture an argument to a function, and the type of
    // that argument will be of type ArrayList<Todo> (we said so earlier
    // using a Mockito annotation like this):
    // @Captor
    // private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;
    // We only want to declare that captor once and let the annotation
    // help us accomplish reassignment of the value for the captor
    // We reset the values of our annotated declarations using the command
    // `MockitoAnnotations.openMocks(this);` in our @BeforeEach

    // Specifically, we want to pay attention to the ArrayList<Todo> that
    // is passed as input when ctx.json is called --- what is the argument
    // that was passed? We capture it and can refer to it later.
    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check that the database collection holds the same number of documents
    // as the size of the captured List<Todo>
    assertEquals(
      db.getCollection("todos").countDocuments(),
      todoArrayListCaptor.getValue().size()
    );
  }

}
