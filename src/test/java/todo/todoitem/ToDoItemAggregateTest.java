package todo.todoitem;

import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.junit.Before;
import org.junit.Test;

public class ToDoItemAggregateTest {
    private FixtureConfiguration fixture;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(ToDoItemAggregate.class);
    }

    @Test
    public void creatingTodoItemsEmitsCreatedEvents() throws Exception {
        fixture.given()
                .when(new CreateToDoItemCommand("1", "Learn Axon"))
                .expectEvents(new ToDoItemCreatedEvent("1", "Learn Axon"));

    }

//    @Test
//    public void markingToDoItemsAsCompleteEmitsCompletedEvents() throws Exception {
//        fixture.given(new ToDoItemCreatedEvent("1", "Learn Axon"))
//                .when(new MarkCompletedCommand("1"))
//                .expectEvents(new ToDoItemCompletedEvent("1"));
//    }
}