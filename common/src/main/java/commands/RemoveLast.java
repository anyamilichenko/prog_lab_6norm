package commands;

import dto.CommandResultDto;
import utilities.CollectionManager;
import utilities.HistoryManager;

public class RemoveLast extends AbstractCommand{
    public RemoveLast(){
        super("", "remove_last");
    }

    @Override
    public CommandResultDto execute(CollectionManager collectionManager, HistoryManager historyManager){
        historyManager.addNote(this.getName());

        collectionManager.getMainData().removeLast();
        return new CommandResultDto("Последний элемент успешно удален");
    }
}
