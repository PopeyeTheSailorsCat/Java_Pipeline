package ved.workers;

import ru.spbstu.pipeline.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

public class Exec implements IExecutor {
    private byte[] what_read;
    private RC STATUS;
    final private Exec_Grammar grammar;// = new Exec_Grammar(gram);
    private boolean decode;
    private int decoder_time=0;
    private IMediator producer_mediator;
    private IConsumer con;
    private My_Mediator mediator;
    private TYPE[] input_types  = {TYPE.BYTE};
    private TYPE[] output_types ={TYPE.CHAR,TYPE.BYTE}; //{TYPE.BYTE,TYPE.CHAR}; //в зависимости от порядка отработают разные вещи
    private int location_in_line=0;
    private int remember_count=0; //Если вдруг в нашей текущей порции и в следующей одинаковые буквы, запоминааем сколько запомнили
    private int buffer_size;
    public Exec(Logger LOGGER){
        STATUS = RC.CODE_SUCCESS;
        Arrays.sort(ExecutorVariables.GrammarToken);
        grammar=new Exec_Grammar(ExecutorVariables.GrammarToken);
    }

    private void write_in_what_read(int counter,byte add_elem){
        String number = Integer.toString(counter);
        for (int i=0;i<number.length();i++){
            what_read[location_in_line]=(byte)number.charAt(i);

            location_in_line++;
            if (location_in_line==buffer_size){
                mediator.my_buffer=what_read;
                STATUS = con.execute();
                what_read=new byte[buffer_size];
                location_in_line=0;

            }
        }

        what_read[location_in_line] = add_elem;

        location_in_line++;
        if (location_in_line==buffer_size){
            //STATUS = con.execute(what_read);
            mediator.my_buffer=what_read;
            STATUS = con.execute();
            what_read=new byte[buffer_size];

            location_in_line=0;
        }

    }
    @Override
    public RC setConfig(String s) {
        Prop_Reader.Set_Grammar(grammar);
        String [] configs= Prop_Reader.Proper_Reader(s);
        if (Prop_Reader.Get_status()!=RC.CODE_SUCCESS){
            STATUS = Prop_Reader.Get_status();
            return STATUS;
        }
        buffer_size=Integer.parseInt(configs[Arrays.binarySearch
                (ExecutorVariables.GrammarToken,ExecutorVariables.Buffer_size)]);
        decode = Boolean.parseBoolean(configs[Arrays.binarySearch
                (ExecutorVariables.GrammarToken,ExecutorVariables.Decode)]);
        what_read = new byte[buffer_size];
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {

        byte [] o= (byte[])producer_mediator.getData();
        if (o==null){
            mediator.my_buffer=null;
            con.execute();
            return RC.CODE_SUCCESS;
        }
        if (!decode){
            byte[] get_line = o;

            int counter=0;
            byte elem;
            elem = get_line[0];
            counter++;
            for(int i=1; i<get_line.length;i++){
                //
                if (elem==get_line[i]){
                    counter++;
                    //System.out.println("was");
                }else{
                    write_in_what_read(counter, elem);
                    //System.out.println("was");
                    if (location_in_line==buffer_size){
                        //STATUS = con.execute(what_read);
                        mediator.my_buffer=what_read;
                        STATUS = con.execute();
                        //
                        what_read=new byte[buffer_size];
                        location_in_line=0;
                    }
                    counter=1;
                    elem=get_line[i];
                }
            }

            if (counter>0){
                //System.out.println("here");
                //System.out.println(location_in_line);
                write_in_what_read(counter,elem);
                byte[] ostatok = new byte [location_in_line];
                System.arraycopy(what_read, 0, ostatok, 0, location_in_line);
                mediator.my_buffer=ostatok;
                STATUS = con.execute();
                location_in_line=0;
                what_read=new byte[buffer_size];
            }
        }else{
            byte[] get_line = o;
            int symbol;
            //int times=0;
            int count = 0;
            byte[] out_line=new byte[buffer_size];
            for (int i =0; i<get_line.length;i++){
                symbol = get_line[i];
                if (symbol>=ExecutorVariables.LowerBoundNumbers &&
                        symbol<=ExecutorVariables.UpperBoundNumbers){
                    decoder_time = decoder_time*10+Character.getNumericValue(symbol);

                }
                    else{

                    for(int j=0;j<decoder_time;j++){
                        out_line[count]=(byte) symbol;
                        count++;
                        if (count==buffer_size){

                            mediator.my_buffer=out_line;
                            //con.execute(out_line);

                            STATUS=con.execute();
                            count=0;
                            out_line = new byte[buffer_size];
                        }
                        //System.out.print((char)symbol);
                    }
                    decoder_time = 0;
                }
            }
            if (count>0){
                byte[] ostatok = new byte [count];
                System.arraycopy(out_line, 0, ostatok, 0, count);
                mediator.my_buffer=ostatok;
                //mediator.my_buffer=out_line;
                STATUS= con.execute();
                //con.execute(out_line);
            }

        }
        return STATUS;
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
        if (iProducer==null){
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }

        TYPE[] prod_type = iProducer.getOutputTypes(); //Получили типы продюсера
        producer_mediator = iProducer.getMediator(MatchTypes(prod_type));
        return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return output_types;
    }
    private TYPE MatchTypes(TYPE[]prod_type){
        //Напишем пересечение множеств
        for (int i=0;i<prod_type.length;i++){
            for (int j=0;j<input_types.length;j++){
                if (input_types[j].equals(prod_type[i])) {
                    return input_types[j];
                }
            }
        }
        return null;//Нужно все покрыть ошибками
    }

    public class My_Mediator implements IMediator{
        public byte[] my_buffer;
        private TYPE required_type;
        public My_Mediator(TYPE type){
            required_type = type;
        }

        private short[] toShort(byte[] data) {
            if (data == null|| data.length % 2 != 0 ) {
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
            if (data == null || data.length % 2 != 0 ) {
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
                    //
                case SHORT:
                    return toShort(my_buffer);
            }
            return null;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        mediator = new My_Mediator(type);
        //System.out.println("Exec Mediator Created");
        return mediator;
    }
}
class Exec_Grammar extends BaseGrammar {

    protected Exec_Grammar(String[] tokens) {
        super(tokens);
    }

}
class ExecutorVariables{
    static final public String Buffer_size = "BUFFER_SIZE";
    static final public String Decode = "DECODE";
    static final public String[] GrammarToken = {"BUFFER_SIZE","DECODE"};
    static final public int LowerBoundNumbers=48;
    static final public int UpperBoundNumbers=57;
}