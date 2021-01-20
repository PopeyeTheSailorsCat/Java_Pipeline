package com.company;

import ru.spbstu.pipeline.*;

import java.util.Arrays;
import java.util.logging.Logger;

public class Exec implements IExecutor {
    private byte[] what_read;
    private RC STATUS;
    //private IConsumer consumer;
    final private String[] gram ={"BUFFER_SIZE","DECODE"};
    final private Exec_Grammar grammar;// = new Exec_Grammar(gram);
    private boolean decode;
    private int decoder_time=0;
    private IMediator producer_mediator;
    private IConsumer con;
    private My_Mediator mediator;
    private TYPE[] my_types ={TYPE.CHAR,TYPE.BYTE}; //{TYPE.BYTE,TYPE.CHAR}; //в зависимости от порядка отработают разные вещи
    private TYPE will_get;
    private int location_in_line=0;
    private int remember_count=0; //Если вдруг в нашей текущей порции и в следующей одинаковые буквы, запоминааем сколько запомнили
    private int buffer_size;
    public Exec(Logger LOGGER){
        STATUS = RC.CODE_SUCCESS;
        Arrays.sort(gram);
        grammar=new Exec_Grammar(gram);
        System.out.println("Exec ready");
    }

    private void write_in_what_read(int counter,byte add_elem){
        if(add_elem == 10){

            return;
        }
        String number = Integer.toString(counter);
        for (int i=0;i<number.length();i++){
            what_read[location_in_line]=(byte)number.charAt(i);

            location_in_line++;
            if (location_in_line==buffer_size){
                //STATUS = con.execute(what_read);
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
        //Наверное следует сделать тут проверку на разумность полученных данных
        buffer_size=Integer.parseInt(configs[Arrays.binarySearch(gram,"BUFFER_SIZE")]);
        decode = Boolean.parseBoolean(configs[Arrays.binarySearch(gram,"DECODE")]);
        what_read = new byte[buffer_size];
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC execute() {
        byte[]o;
        if (will_get==TYPE.BYTE){
            o= (byte[])producer_mediator.getData();
            if (o==null){
                mediator.my_buffer=null;
                con.execute();
                return RC.CODE_SUCCESS;
            }
        }else { //if(will_get==TYPE.CHAR)
            char[] ch  =(char[]) producer_mediator.getData();
            if (ch==null){
                mediator.my_buffer=null;
                STATUS=con.execute();
                return STATUS;
            }
            o=new byte[ch.length];

            for (int i=0;i<ch.length;i++){
                o[i]=(byte)ch[i];
                //System.out.println(ch[i]);
            }
        }

        //  byte[] o= (byte[])producer_mediator.getData(); //Переписать получение типа

        //  byte[] o= (byte[])producer_mediator.getData(); //Переписать получение типа

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
                mediator.my_buffer=what_read;
                STATUS = con.execute();
                location_in_line=0;
                what_read=new byte[buffer_size];
            }
        }else{
            byte[] get_line = o;
            int symbol;
            //int times=0;
            int count = 0;
//            while ((symbol= get_line.read())!=-1){
//                if (symbol>=48 && symbol<=57){
//                    times = times*10+Character.getNumericValue(symbol);
//                }else{
//                    for(int i=0;i<times;i++){
//                        System.out.print((char)symbol);
//                    }
//                    times=0;
//                }
//            }
            byte[] out_line=new byte[buffer_size];
            for (int i =0; i<get_line.length;i++){
                symbol = get_line[i];
                if (symbol>=48 && symbol<=57){
                    decoder_time = decoder_time*10+Character.getNumericValue(symbol);

                }
                    else{
                    //System.out.println(times);

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

                mediator.my_buffer=out_line;
                STATUS= con.execute();
                //con.execute(out_line);
            }

        }
        return STATUS;
    }

//    @Override
//    public RC execute() {
//        return null;
//    }

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
        will_get=MatchTypes(prod_type);

        return RC.CODE_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return my_types;
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

    public class My_Mediator implements IMediator{
        public byte[] my_buffer;
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
                    //
            }
            return null;
        }
    }

    @Override
    public IMediator getMediator(TYPE type) {
        mediator = new My_Mediator(type);
        System.out.println("Exec Mediator Created");
        return mediator;
    }
}
class Exec_Grammar extends BaseGrammar {

    protected Exec_Grammar(String[] tokens) {
        super(tokens);
    }

}
