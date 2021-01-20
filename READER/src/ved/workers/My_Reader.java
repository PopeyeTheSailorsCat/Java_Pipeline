package ved.workers;


import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class My_Reader implements IReader{
    final private Reader_Grammar grammar;// = new Reader_Grammar(gram);
    final private Logger logger;
    private RC STATUS;
    private My_Mediator mediator;

    private TYPE[] output_types = {TYPE.CHAR,TYPE.BYTE, TYPE.SHORT}; //{TYPE.BYTE,TYPE.CHAR};
    private FileInputStream input_path;
    private int buffer_size;
    private IConsumer con;
    public My_Reader(Logger LOGGER) {
        logger = LOGGER;
        Arrays.sort(Reader_Variables.GrammarToken);
        grammar=new Reader_Grammar(Reader_Variables.GrammarToken);
        //System.out.println("Reader ready to work");
    }


    public RC setConfig(String var1) {

        Prop_Reader.Set_Grammar(grammar);
        String [] configs= Prop_Reader.Proper_Reader(var1);
        buffer_size=Integer.parseInt(configs
                [Arrays.binarySearch(Reader_Variables.GrammarToken,Reader_Variables.Buffer_size)]);
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
        return output_types;
    }

    public class My_Mediator implements IMediator{
        public byte[] my_buffer =new byte[buffer_size];
        private TYPE required_type;
        public My_Mediator(TYPE type){
            required_type = type;
        }

        private short[] toShort(byte[] data) {
            if (data == null || data.length % 2 != 0) {
                return null;
            } else {
                short[] result = new short[data.length / 2];
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);

                for(int i = 0; i < data.length; ++i) {
                    result[i] = byteBuffer.getShort(2 * i);
                }

                return result;
            }
        }

        private char[] toChar(byte[] data) {
            if (data == null) {
                return null;
            } else {
                short[] tmp_short = toShort(data);
                char[] result_char = new char[tmp_short.length];

                for(int i = 0; i < tmp_short.length; ++i) {
                    result_char[i] = (char)tmp_short[i];
                }

                return result_char;
            }
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
                    return toChar(my_buffer);
                case SHORT:
                    return toShort(my_buffer);
            }
            return null;
        }
    }
    @Override
    public IMediator getMediator(TYPE type) {
        mediator = new My_Mediator(type);
        //System.out.println("Reader Mediator Created");
        return mediator;
    }
}
class Reader_Grammar extends BaseGrammar {

    protected Reader_Grammar(String[] tokens) {
        super(tokens);
    }

}
class Reader_Variables{
    static final public String Buffer_size = "BUFFER_SIZE";
    static final public String[] GrammarToken = {"BUFFER_SIZE"};
}
