package commands;

import data.Dragon;
import dto.CommandResultDto;
import utilities.CollectionManager;
import utilities.HistoryManager;

public class AddCommand extends AbstractCommand{
    public AddCommand(Dragon arg){
        super(arg, "add");
    }

    @Override
    public CommandResultDto execute(CollectionManager collectionManager, HistoryManager historyManager){
        historyManager.addNote((this.getName()));
        Dragon dragon = (Dragon) arg;
        dragon.setId(CollectionManager.getMaxId() + 1L); //С getmaxid проблемы, возможно будут проблемы с добавлением элементов
        collectionManager.getMainData().add(dragon);
        return new CommandResultDto("Элемент успешно добавлен");
    }
}
