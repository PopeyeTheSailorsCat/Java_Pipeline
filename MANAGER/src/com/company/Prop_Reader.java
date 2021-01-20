package com.company;

import ru.spbstu.pipeline.BaseGrammar;
import ru.spbstu.pipeline.RC;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class Prop_Reader {
    static private RC STATUS;
    static private BaseGrammar gram;
    public static String[] Proper_Reader(String filename) {
        STATUS=RC.CODE_SUCCESS;

        try (FileReader reader = new FileReader(filename);
             Scanner scanner = new Scanner(reader)) {
            String[] readed_conf = new String[gram.numberTokens()];
            while (scanner.hasNextLine()) {
                String line[] = scanner.nextLine().split(gram.delimiter());
                ProcessLine(line,readed_conf);
            }
            return readed_conf;

        }
        catch (IOException exc) {
            //Errors.OPEN_FILE_ERROR.PrintError();
            STATUS = RC.CODE_FAILED_TO_READ;
        }
        return new String[]{""};
    }
    public static RC Get_status(){
        return STATUS;
    }
    public static void Set_Grammar(BaseGrammar o){
        gram = o;
        //System.out.println("set grammar");
    }

    private static void ProcessLine(String[] line, String[] find) {
        if (line.length != 2) {
           //Errors.WRONG_CONFIG_DELIMITER.PrintError();
            STATUS = RC.CODE_CONFIG_GRAMMAR_ERROR;
            return;
        }
        for (int i = 0; i < line.length; ++i) {
            line[i] = line[i].replaceAll(" ", "");
            for (int j = 0; j < gram.numberTokens(); j++) {
                if (line[0].equals(gram.token(j))) {
                    find[j] = line[1];
                }
            }
        }
    }

}