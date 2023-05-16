package commands;

import dto.CommandResultDto;
import utilities.CollectionManager;
import utilities.HistoryManager;

public class ClearCommand extends AbstractCommand {
    public ClearCommand() {
        super("", "clear");
    }

    @Override
    public CommandResultDto execute(
            CollectionManager collectionManager,
            HistoryManager historyManager
    ) {
        historyManager.addNote(this.getName());
        // stream api would not help
        collectionManager.getMainData().clear();
        return new CommandResultDto("Коллекция была успешно очищена.");
    }
}
