package commands;

import data.Dragon;
import dto.CommandResultDto;
import utilities.CollectionManager;
import utilities.HistoryManager;

import java.util.LinkedList;
import java.util.Objects;

public class CountGreater extends AbstractCommand{
    public CountGreater(String arg){
        super(arg, "count_greater_than_age");
    }

    @Override
    public CommandResultDto execute(CollectionManager collectionManager, HistoryManager historyManager){
        historyManager.addNote(this.getName());
        long age = Long.parseLong(((String) arg).trim());
        LinkedList<Dragon> dragons = collectionManager.getMainData();
        System.out.println("количество элементов, значение поля age которых больше заданного: ");

        return new CommandResultDto(dragons.stream()
                .filter(Objects::nonNull)
                .filter(s -> Long.compare(s.getAge(), age) <= -1)
                .map(Objects::toString)
                .count());
    }
}