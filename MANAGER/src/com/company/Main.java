package com.company;

import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static void main(String[] args)  {
        RC STATUS;
        if(args == null || args.length == 0)
        {
            System.out.println(RC.CODE_INVALID_ARGUMENT.toString());
            //Errors.WRONG_ARGS.PrintError();
            System.out.println("Proper Usage is: java program filename");
            System.exit(1);
        }
        My_Manager manager = new My_Manager(args[0],LOGGER);
        STATUS=manager.Get_status();
        if (STATUS!=RC.CODE_SUCCESS){
            return;
        }
        manager.give_task();
        STATUS=manager.Get_status();
        if (STATUS!=RC.CODE_SUCCESS){
            return;
        }
        manager.run_pipeline();
//
    }
}
