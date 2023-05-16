package commands;

import data.Dragon;
import dto.CommandResultDto;
import utilities.CollectionManager;
import utilities.HistoryManager;

public class UpdateCommand extends AbstractCommand {

    private final String idArg;

    public UpdateCommand(Dragon dragon, String id) {
        super(dragon, "update");
        this.idArg = id;
    }

    @Override
    public CommandResultDto execute(CollectionManager collectionManager, HistoryManager historyManager){
        historyManager.addNote(this.getName());
        Long longArg;
        try {
            longArg = Long.parseLong(idArg);
        }catch (NumberFormatException e){
            return new CommandResultDto("Ваш аргумент некорректен. Команда не выполнена");
        }
        if (collectionManager.getMainData().removeIf(x -> x.getID() == longArg)){
            Dragon dragon = (Dragon) arg;
            dragon.setId(longArg);
            collectionManager.getMainData().add(dragon);
            return new CommandResultDto("Элемент был успешно обновлен");
        }else {
            return new CommandResultDto("Идентификатор не найден. Команда не была выполнена");
        }
    }
}
