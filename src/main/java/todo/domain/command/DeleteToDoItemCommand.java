package todo.domain.command;

import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;

import lombok.NonNull;
import lombok.Value;

@Value
public class DeleteToDoItemCommand {
    @TargetAggregateIdentifier
    @NonNull private final String todoId;

}
