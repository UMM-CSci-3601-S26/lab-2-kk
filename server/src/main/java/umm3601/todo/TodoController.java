package umm3601.todo;

import static com.mongodb.client.model.Filters.and;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import umm3601.Controller;

public class TodoController implements Controller {

  private static final String API_TODOS = "/api/todos";

  private final JacksonMongoCollection<Todo> todoCollection;

  /**
   * Construct a controller for todos.
   *
   * @param database the database containing todo data
   */
  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  /**
   * Set the JSON body of the response to be a list of all the todos returned from the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodos(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    // Bson sortingOrder = constructSortingOrder(ctx);

    // All three of the find, sort, and into steps happen "in parallel" inside the
    // database system. So MongoDB is going to find the todos with the specified
    // properties, return those sorted in the specified manner, and put the
    // results into an initially empty ArrayList.
    ArrayList<Todo> matchingTodos = todoCollection
      .find(combinedFilter)
      //.sort(sortingOrder)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of todos returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingTodos);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
  }


  /**
   * Construct a Bson filter document to use in the `find` method based on the
   * query parameters from the context.
   *
   * This checks for the presence of the various query
   * parameters and constructs a filter document that will match todos with
   * the specified values for those fields.
   *
   * @param ctx a Javalin HTTP context, which contains the query parameters
   *    used to construct the filter
   * @return a Bson filter document that can be used in the `find` method
   *   to filter the database collection of todos
   */
  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with an empty list of filters

    // if (ctx.queryParamMap().containsKey(AGE_KEY)) {
    //   int targetAge = ctx.queryParamAsClass(AGE_KEY, Integer.class)
    //     .check(it -> it > 0, "User's age must be greater than zero; you provided " + ctx.queryParam(AGE_KEY))
    //     .check(it -> it < REASONABLE_AGE_LIMIT,
    //       "User's age must be less than " + REASONABLE_AGE_LIMIT + "; you provided " + ctx.queryParam(AGE_KEY))
    //     .get();
    //   filters.add(eq(AGE_KEY, targetAge));
    // }
    // if (ctx.queryParamMap().containsKey(COMPANY_KEY)) {
    //   Pattern pattern = Pattern.compile(Pattern.quote(ctx.queryParam(COMPANY_KEY)), Pattern.CASE_INSENSITIVE);
    //   filters.add(regex(COMPANY_KEY, pattern));
    // }
    // if (ctx.queryParamMap().containsKey(ROLE_KEY)) {
    //   String role = ctx.queryParamAsClass(ROLE_KEY, String.class)
    //     .check(it -> it.matches(ROLE_REGEX), "User must have a legal user role")
    //     .get();
    //   filters.add(eq(ROLE_KEY, role));
    // }

    // Combine the list of filters into a single filtering document.
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  @Override
  public void addRoutes(Javalin server) {
    // List todos, filtered using query parameters
    server.get(API_TODOS, this::getTodos);
  }
}
