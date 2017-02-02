package todo.web;

import static java.util.Optional.of;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import todo.domain.ToDoItem;
import todo.domain.command.ClearTodoListCommand;
import todo.domain.command.CreateToDoItemCommand;
import todo.domain.command.DeleteToDoItemCommand;
import todo.domain.command.UpdateToDoItemCommand;
import todo.middleware.CompletionTracker;
import todo.query.TodoQueryService;
import todo.view.ToDoItemView;
import todo.view.ToDoItemViewFactory;

@Slf4j
@RestController
@RequestMapping("/todos")
public class ToDoController {
    public static final String TODO_URL = "/{id}";
    private static final String USER_ID = "1";
    
    private final CommandGateway commandGateway;
    private final TodoQueryService queryService;
    private final ToDoItemViewFactory viewFactory;
    private final CompletionTracker completionTracker;

    @Autowired
    public ToDoController(@NonNull CommandGateway commandGateway, @NonNull TodoQueryService queryService, @NonNull ToDoItemViewFactory toDoItemViewFactory, CompletionTracker completionTracker) {
		this.commandGateway = commandGateway;
        this.queryService = queryService;
        this.viewFactory = toDoItemViewFactory;
        this.completionTracker = completionTracker;
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<ToDoItemView> index() {
        return viewFactory.buildList( queryService.queryListForUser( USER_ID));
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ToDoItemView> create(@RequestBody ToDoItemView todo) throws Throwable {
        String itemId = UUID.randomUUID().toString();

        todo.setCompleted(false);
        todo.setId( itemId);
        
        String trackerId = UUID.randomUUID().toString();
        CompletableFuture<ToDoItem> future = new CompletableFuture<ToDoItem>();
        completionTracker.getItemTracker().addTracker(trackerId, future);
        try {
        	commandGateway.sendAndWait( new CreateToDoItemCommand( USER_ID, itemId, todo.getTitle(), todo.getCompleted(), todo.getOrder(), of( trackerId)),
        			1, TimeUnit.SECONDS);
			ToDoItem item = future.get(1, TimeUnit.SECONDS);
			ToDoItemView result = viewFactory.buildItem( item);
	        return new ResponseEntity<>( result, HttpStatus.CREATED);
		} catch (CommandExecutionException e) {
			if( e.getCause() != null) {
				throw e.getCause();
			}
			log.error( "Got CommandExecutionException with no underlying cause", e);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log.error( "Could not retrieve response to render output", e);
		}
        return new ResponseEntity<ToDoItemView>( HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Collection<ToDoItemView>> clear() {
        String trackerId = UUID.randomUUID().toString();
        CompletableFuture<Collection<ToDoItem>> future = new CompletableFuture<Collection<ToDoItem>>();
        completionTracker.getListTracker().addTracker(trackerId, future);
        commandGateway.send( new ClearTodoListCommand( USER_ID, of( trackerId)));        
        try {
        	Collection<ToDoItem> items = future.get(1, TimeUnit.SECONDS);
			List<ToDoItemView> result = viewFactory.buildList( items);
	        return new ResponseEntity<>( result, HttpStatus.OK);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log.error( "Could not retrieve response to render output", e);
			return new ResponseEntity<Collection<ToDoItemView>>( HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @RequestMapping(value = TODO_URL, method = RequestMethod.GET)
    public @ResponseBody ToDoItemView show(@PathVariable String id) {
        return viewFactory.buildItem( queryService.queryListForItem(USER_ID, id));
    }

    @RequestMapping(value = TODO_URL, method = RequestMethod.PATCH)
    public ResponseEntity<ToDoItemView> update(@PathVariable String id, @RequestBody ToDoItemView todo) throws Throwable {        
        String trackerId = UUID.randomUUID().toString();
        CompletableFuture<ToDoItem> future = new CompletableFuture<ToDoItem>();
        completionTracker.getItemTracker().addTracker(trackerId, future);
        try {
        	commandGateway.sendAndWait( new UpdateToDoItemCommand( USER_ID, id, todo.getTitle(), todo.getCompleted(), todo.getOrder(), of( trackerId)),
        			1, TimeUnit.SECONDS);
			ToDoItem item = future.get(1, TimeUnit.SECONDS);
			ToDoItemView result = viewFactory.buildItem( item);
	        return new ResponseEntity<>( result, HttpStatus.OK);
		} catch (CommandExecutionException e) {
			if( e.getCause() != null) {
				throw e.getCause();
			}
			log.error( "Got CommandExecutionException with no underlying cause", e);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log.error( "Could not retrieve response to render output", e);
		}
        return new ResponseEntity<ToDoItemView>( HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = TODO_URL, method = RequestMethod.DELETE)
    public ResponseEntity<String> delete(@PathVariable String id) throws Throwable {
        String trackerId = UUID.randomUUID().toString();
        CompletableFuture<ToDoItem> future = new CompletableFuture<ToDoItem>();
        completionTracker.getItemTracker().addTracker(trackerId, future);
        try {
        	commandGateway.sendAndWait( new DeleteToDoItemCommand( USER_ID, id, of( trackerId)),
        			1, TimeUnit.SECONDS);
			ToDoItem item = future.get(1, TimeUnit.SECONDS);
	        return new ResponseEntity<>( "{}", HttpStatus.OK);
		} catch (CommandExecutionException e) {
			if( e.getCause() != null) {
				throw e.getCause();
			}
			log.error( "Got CommandExecutionException with no underlying cause", e);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log.error( "Could not retrieve response to render output", e);
		}
        return new ResponseEntity<String>( HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
