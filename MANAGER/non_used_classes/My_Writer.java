package com.company;

import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class My_Writer implements IWriter {
    private byte[] i_know;
    private int buffer_size;
    private RC STATUS;
    private Logger logger;
    private IMediator producer_mediator;
    private TYPE[] my_types = {TYPE.BYTE,TYPE.CHAR};
    private TYPE will_get;
    private int count_symbols_in_buffer=0;
    final private String[] gram ={"BUFFER_SIZE"};
    final private Writer_Grammar grammar;// = new Writer_Grammar(gram);
    private FileOutputStream output;
    public My_Writer(Logger LOGGER)  {
        STATUS=RC.CODE_SUCCESS;
        logger=LOGGER;
        Arrays.sort(gram);
        grammar=new Writer_Grammar(gram);
        System.out.println("Writer ready to work");
    }

    @Override
    public RC setConfig(String s) {
        Prop_Reader.Set_Grammar(grammar);
        String [] configs= Prop_Reader.Proper_Reader(s);
        if (Prop_Reader.Get_status()!=RC.CODE_SUCCESS){
            STATUS = Prop_Reader.Get_status();
            return STATUS;
        }
        //Наверное следует сделать тут проверку на разумность полученных данных
        buffer_size=Integer.parseInt(configs[Arrays.binarySearch(gram,"BUFFER_SIZE")]);
        i_know= new byte[buffer_size];
        return RC.CODE_SUCCESS;
    }


    @Override
    public RC execute() {
        //byte[] get_line=(byte[])producer_mediator.getData();; //Может поставить копирование
        byte[]get_line;
        if (will_get==TYPE.BYTE){
            get_line= (byte[])producer_mediator.getData();
            if (get_line==null){
                return RC.CODE_SUCCESS;
            }
        }else { //if(will_get==TYPE.CHAR)

            char[] ch = (char[]) producer_mediator.getData();
            if(ch==null){
                return RC.CODE_SUCCESS;
            }
            get_line = new byte[ch.length];

            for (int i = 0; i < ch.length; i++) {
                get_line[i] = (byte) ch[i];
                //System.out.println(ch[i]);
            }
        }
        try {

            for (int i=0; i<get_line.length;i++){
                if (get_line[i]!=0 && get_line[i]!=10 ){
                    i_know[count_symbols_in_buffer]=get_line[i];
                    count_symbols_in_buffer++;
                }

                if (count_symbols_in_buffer== buffer_size) {
                    output.write(i_know,0,buffer_size);
                    i_know = new byte[buffer_size];
                    count_symbols_in_buffer=0;
                }
            }

            if (count_symbols_in_buffer>0){
                byte[] ostatok = new byte[count_symbols_in_buffer];
                for (int i=0;i<count_symbols_in_buffer;i++){
                    ostatok[i]=i_know[i];
                }
                output.write(ostatok,0,count_symbols_in_buffer);
                i_know = new byte[buffer_size];
                count_symbols_in_buffer=0;
            }

            //return RC.CODE_SUCCESS;

        }catch (IOException ex){
            //Errors.WRITER_ERROR.PrintError();
            //System.out.println("here");
            STATUS = RC.CODE_FAILED_TO_WRITE;
            logger.log(Level.WARNING,STATUS.toString());
            return STATUS;
        }

        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setOutputStream(FileOutputStream fileOutputStream) {
        output=fileOutputStream;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer iProducer) {
        if (iProducer==null){
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }

        TYPE[] prod_type = iProducer.getOutputTypes(); //Получили типы продюсера
        producer_mediator = iProducer.getMediator(MatchTypes(prod_type));
        will_get = MatchTypes(prod_type);
        return RC.CODE_SUCCESS;
    }
    private TYPE MatchTypes(TYPE[]prod_type){
        //Напишем пересечение множеств
        for (int i=0;i<prod_type.length;i++){
            for (int j=0;j<my_types.length;j++){
                if (my_types[j].equals(prod_type[i])) {
                    return my_types[j];
                }
            }
        }
        return null;//Нужно все покрыть ошибками
    }
}
class Writer_Grammar extends BaseGrammar {

    protected Writer_Grammar(String[] tokens) {
        super(tokens);
    }


}