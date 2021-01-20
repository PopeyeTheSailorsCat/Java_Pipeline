package com.company;

import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class My_Manager{
    private Manager_Grammar grammar;// = new Manager_Grammar();
    private IReader Pipeline_reader;//Reader по сути
    private final Logger logger;
    private String[] workers_class;
    private String[] workers_configs;
    private final String [] configs;
    RC STATUS;
    public My_Manager(String conf_loc, Logger LOGGER){
        STATUS = RC.CODE_SUCCESS;
        logger = LOGGER;
        Arrays.sort(Manager_Variables.ManagerGrammarToken);
        grammar=new Manager_Grammar(Manager_Variables.ManagerGrammarToken);
        Prop_Reader.Set_Grammar(grammar);
        configs= Prop_Reader.Proper_Reader(conf_loc);
        if (Prop_Reader.Get_status()!=RC.CODE_SUCCESS){
            STATUS = Prop_Reader.Get_status();
            return;
        }

        int number_of_workers = Integer.parseInt(configs
                [Arrays.binarySearch(Manager_Variables.ManagerGrammarToken,Manager_Variables.Workers)]);//Количество работников на линии
        String[] Workers= new String[number_of_workers];
        String[] Configs = new String[number_of_workers];
        Manager_Variables.WriterPosition = number_of_workers-1;
        for (int i = 0;i<number_of_workers;i++){//Создаем грамматику чтобы вытащить рабочих и их конфиги
            Workers[i]=Manager_Variables.Worker + (i + 1);
            Configs[i]=Manager_Variables.Config + (i + 1);

        }
        grammar=new Manager_Grammar(Workers);
        Prop_Reader.Set_Grammar(grammar);
        workers_class= Prop_Reader.Proper_Reader(conf_loc);//Вытаскиваем имена классов рабочих
        if (Prop_Reader.Get_status()!=RC.CODE_SUCCESS){
            STATUS = Prop_Reader.Get_status();
            return;
        }

        grammar=new Manager_Grammar(Configs);
        Prop_Reader.Set_Grammar(grammar);
        workers_configs = Prop_Reader.Proper_Reader(conf_loc);//Вытаскиваем конфиги рабочих
        if (Prop_Reader.Get_status()!=RC.CODE_SUCCESS){
            STATUS = Prop_Reader.Get_status();
        }
    }
    private void build_pipeline(String[] worker_class, String[] worker_conf){
        int number_of_workers = worker_class.length;
        IPipelineStep[] pipeline = new IPipelineStep[number_of_workers];
        pipeline[Manager_Variables.ReaderPosition] = create_pipeline_step(worker_class[Manager_Variables.ReaderPosition],
                worker_conf[Manager_Variables.ReaderPosition],Manager_Variables.ReaderType);

        pipeline[Manager_Variables.WriterPosition] = create_pipeline_step(worker_class[Manager_Variables.WriterPosition],
                worker_conf[Manager_Variables.WriterPosition],Manager_Variables.WriterType);
        for (int i = 1; i < number_of_workers-1; ++i) {
            pipeline[i] = create_pipeline_step(worker_class[i],worker_conf[i],Manager_Variables.ExecutorType);
        }
        try {
            try {
                ((IReader) pipeline[Manager_Variables.ReaderPosition]).setInputStream( new FileInputStream(configs
                        [Arrays.binarySearch(Manager_Variables.ManagerGrammarToken,Manager_Variables.InputStream)]));
            } catch (FileNotFoundException e) {
                STATUS = RC.CODE_INVALID_INPUT_STREAM;
                logger.log(Level.WARNING,STATUS.toString());
                return ;
            }
            try {
                ((IWriter) pipeline[Manager_Variables.WriterPosition]).setOutputStream(new FileOutputStream(configs
                        [Arrays.binarySearch(Manager_Variables.ManagerGrammarToken,Manager_Variables.OutputSteam)]));
            } catch (FileNotFoundException e) {
                STATUS = RC.CODE_INVALID_OUTPUT_STREAM;
                logger.log(Level.WARNING,STATUS.toString());
                return;
            }
        } catch (ClassCastException ex) {
            STATUS = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            logger.log(Level.WARNING,STATUS.toString());
            return;
        }
        STATUS=Create_Dependences(pipeline);
    }


    private RC Create_Dependences(IPipelineStep[] pipeline){
        for (int i = 0; i < pipeline.length - 1; ++i) {
            if (i == Manager_Variables.ReaderPosition){
                //STATUS=((IPipelineStep)pipeline[i]).setProducer(null); С некоторыми ридерами конфликт

            }
            else {
                STATUS =((IPipelineStep) pipeline[i]).setProducer((IProducer) pipeline[i - 1]);
           }
            if (STATUS != RC.CODE_SUCCESS)
                return STATUS;
            //System.out.println(i);
                STATUS = ((IPipelineStep) pipeline[i]).setConsumer((IConsumer) pipeline[i + 1]);
                if (STATUS != RC.CODE_SUCCESS){

                    return STATUS;
                }


        }
        STATUS =((IPipelineStep) pipeline[Manager_Variables.WriterPosition]).
                setProducer((IProducer) pipeline[Manager_Variables.WriterPosition - 1]);
        Pipeline_reader =(IReader) pipeline[Manager_Variables.ReaderPosition];

        return STATUS;
    }
    private IPipelineStep create_pipeline_step(String className, String configPath,int type) {
        IPipelineStep worker = null;
        try {
            //Рефлексируем наш класс
            //System.out.println(className);
            Class cl = Class.forName(className);
            Class[] params = {Logger.class};
            switch (type){
                case Manager_Variables.ExecutorType:
                    worker = ((IExecutor) cl.getConstructor(params).newInstance(logger));
                        break;
                case Manager_Variables.ReaderType:
                    worker = ((IReader) cl.getConstructor(params).newInstance(logger));
                    //System.out.println("create Reader");
                    break;
                case Manager_Variables.WriterType:
                    worker = ((IWriter) cl.getConstructor(params).newInstance(logger));
                    break;
            }

            STATUS = ((IConfigurable)worker).setConfig(configPath);
            if (STATUS != RC.CODE_SUCCESS)
                return null;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InstantiationException | InvocationTargetException e) {
            STATUS = RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            logger.log(Level.WARNING,STATUS.toString());
            return null;

        }
        return worker;
    }

    public void give_task(){
            build_pipeline(workers_class,workers_configs);
    }


    public void run_pipeline(){
        Pipeline_reader.execute();
    }
    public RC Get_status(){
        return STATUS;
    }
}
class Manager_Grammar extends BaseGrammar {

    protected Manager_Grammar(String[] tokens) {
        super(tokens);
    }


}

class Manager_Variables{
    static final public String InputStream = "INPUT_STREAM";
    static final public String OutputSteam="OUTPUT_STREAM";
    static final public String Workers="WORKERS";
    static final public String Worker = "WORKER_";
    static final public String Config = "CONF_";
    static final public String[] ManagerGrammarToken = {"INPUT_STREAM","OUTPUT_STREAM","WORKERS"};
    static final public int ReaderType=1;
    static final public int WriterType=2;
    static final public int ExecutorType=0;
    static final public int ReaderPosition=0;
    static public int WriterPosition = -1;
}
