package com.company;


import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class My_Reader implements IReader{
    final private String[] gram ={"BUFFER_SIZE"};
    final private Reader_Grammar grammar;// = new Reader_Grammar(gram);
    final private Logger logger;
    private RC STATUS;
    private My_Mediator mediator;
    private TYPE[] my_types = {TYPE.CHAR,TYPE.BYTE}; //{TYPE.BYTE,TYPE.CHAR};
    private FileInputStream input_path;
    private int buffer_size;
    private IConsumer con;
    public My_Reader(Logger LOGGER) {
        logger = LOGGER;
        Arrays.sort(gram);
        grammar=new Reader_Grammar(gram);
        System.out.println("Reader ready to work");
    }


    public RC setConfig(String var1) {

        Prop_Reader.Set_Grammar(grammar);
        String [] configs= Prop_Reader.Proper_Reader(var1);
        buffer_size=Integer.parseInt(configs[Arrays.binarySearch(gram,"BUFFER_SIZE")]);
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {
        int byte_counter;
        byte[] tmp = new byte[buffer_size + 1];
        while (true) {
            try {
                if (!((byte_counter = input_path.read(tmp, 0, buffer_size)) != -1)) break;
            } catch (IOException e) {
                logger.log(Level.WARNING,STATUS.toString());
                return RC.CODE_FAILED_TO_READ;
            }
            if (byte_counter<buffer_size){
                mediator.my_buffer=new byte[byte_counter];
            }
            System.arraycopy(tmp, 0, mediator.my_buffer, 0, byte_counter);
            //STATUS = con.execute(buffer);
            STATUS = con.execute();

        }
        mediator.my_buffer=null;
        STATUS=con.execute();
        return STATUS;
    }

    @Override
    public RC setInputStream(FileInputStream var1) {
        input_path=var1;//Копирую путь на вход
        return RC.CODE_SUCCESS;
    }


    @Override
    public RC setConsumer(IConsumer iConsumer) {
        if (iConsumer==null){
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        con=iConsumer;//Получаю ссылку на потребителя
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setProducer(IProducer iProducer) {
       return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return my_types;
    }

    public class My_Mediator implements IMediator{
        public byte[] my_buffer =new byte[buffer_size];
        private TYPE required_type;
        public My_Mediator(TYPE type){
            required_type = type;
        }
        private char[] DataToChar(){
            char[] result = new char[my_buffer.length];
            for (int i =0;i<my_buffer.length;i++){
                result[i]=(char)my_buffer[i];
            }
            return result;
        }
        @Override
        public Object getData() {
            if (required_type==null){
                return null;
            }
            if (my_buffer==null) {
                return null;
            }
            switch (required_type){
                case BYTE:
                    return my_buffer;
                case CHAR:
                    return DataToChar();
            }
            return null;
        }
    }
    @Override
    public IMediator getMediator(TYPE type) {
        mediator = new My_Mediator(type);
        System.out.println("Reader Mediator Created");
        return mediator;
    }
}
class Reader_Grammar extends BaseGrammar {

    protected Reader_Grammar(String[] tokens) {
        super(tokens);
    }

}
