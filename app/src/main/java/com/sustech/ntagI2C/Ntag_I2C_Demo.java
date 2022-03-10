package com.sustech.ntagI2C;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.sustech.ntagI2C.activities.AuthActivity;
import com.sustech.ntagI2C.activities.RegisterConfigActivity;
import com.sustech.ntagI2C.activities.RegisterSessionActivity;
import com.sustech.ntagI2C.exceptions.CommandNotSupportedException;
import com.sustech.ntagI2C.exceptions.NotPlusTagException;
import com.sustech.ntagI2C.reader.I2C_Enabled_Commands;
import com.sustech.ntagI2C.reader.Ntag_Get_Version;
import com.sustech.ntagI2C.reader.Ntag_I2C_Commands;
import com.sustech.ntagI2C.reader.Ntag_I2C_Plus_Registers;
import com.sustech.ntagI2C.reader.Ntag_I2C_Registers;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class Ntag_I2C_Demo{

    private I2C_Enabled_Commands reader;
    private MainActivity main;
    private Tag tag;
    private LedTask lTask;

    private int times = 0;


    //DEFINES
    private static final int DELAY_TIME = 200;

    public Ntag_I2C_Demo(Tag tag, final Activity main, final byte[] passwd, final int authStatus){
        if (tag == null) {
            this.main = null;
            this.tag = null;
        }
        this.main = (MainActivity) main;
        this.tag = tag;


        try {
            if(tag == null) { Log.v("Ntag_I2C_Demo",":tag is null");}
            reader = I2C_Enabled_Commands.get(tag);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (reader == null) {
            String message = "The Tag could not be identified or this NFC device does not"
                    + "support the NFC Forum commnds needed to access this tag";
            String title = "Communication failed";
            showAlert(message, title);
        } else {
            try {
                reader.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void showAlert(final String message, final String title) {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(main)
                        .setMessage(message)
                        .setTitle(title)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                    }
                                }).show();
            }
        });
    }

    public void LED() {
        lTask = new LedTask();
        lTask.execute();
    }



    private class LedTask extends AsyncTask<Void, Byte[], Void> {
        private final byte deviceToTag = 1;
        private final byte tagToDevice = 2;
        private final byte noTransfer = 0;
        private final byte invalidTransfer = 4;

        private Boolean exit = false;


        @Override
        protected Void doInBackground(Void... voids) {
            byte[] dataTx = new byte[reader.getSRAMSize()];
            byte[] dataRx = new byte[reader.getSRAMSize()];
            Byte[][] result;

            //we have to make sure the pass-through mode is activated
            long RegTimeOutStart = System.currentTimeMillis();
            boolean RTest = false;
            try {
                do {

                    if (reader.checkPTwritePossible()) {
                        break;
                    }

                    long RegTimeout = System.currentTimeMillis();
                    RegTimeout = RegTimeout - RegTimeOutStart;
                    RTest = (RegTimeout < 5000);
                } while (RTest);

                while (true) {
                    dataTx[reader.getSRAMSize()-4] = (byte)HomeFragment.LED;
                    times++;
                    if (main.isSram0to31Enabled()) {
                        dataTx[reader.getSRAMSize() - 2] = 'E';
                    }
                    else
                    {
                        dataTx[reader.getSRAMSize() - 2] = 0x00;
                    }

                    if (main.isSram32to61Enabled()) {
                        dataTx[reader.getSRAMSize() - 1] = 'E';
                    }
                    else
                    {
                        dataTx[reader.getSRAMSize() - 1] = 0x00;
                    }

                    Log.v("Ntag_I2C_Demo","第一次：deviceToTag");
                    displayTransferDir(deviceToTag);

                    //wait to prevent that a RF communication is at the same time as uC I2C
                    Thread.sleep(10);
                    reader.waitforI2Cread(DELAY_TIME);
                    reader.writeSRAMBlock(dataTx, null);
                    Log.v("Ntag_I2C_Demo","第二次：tagToDevice");
                    displayTransferDir(tagToDevice);

                    //wait to prevent that a RF communication is at the same time as uC I2C
                    Thread.sleep(10);
                    //reader.waitforI2Cwrite(100);
                    reader.waitforI2Cwrite(200);
                    dataRx = reader.readSRAMBlock();


                    if (exit) {
                        dataTx[reader.getSRAMSize() - 1] = 0x00;
                        dataTx[reader.getSRAMSize() - 2] = 0x00;

                        //wait to prevent that a RF communication is at the same time as uC I2C
                        Thread.sleep(10);
                        reader.waitforI2Cread(100);
                        reader.writeSRAMBlock(dataTx, null);

                        //wait to prevent that a RF communication is at the same time as uC I2C
                        Thread.sleep(10);
                        reader.waitforI2Cwrite(100);
                        dataRx = reader.readSRAMBlock();

                        cancel(true);
                        return null;
                    }

                    //Convert byte[] to Byte[]
                    Byte[] bytes = new Byte[dataRx.length];
                    for (int i = 0; i < dataRx.length; i++) {
                        bytes[i] = Byte.valueOf(dataRx[i]);
                    }
                    result = new Byte[2][];
                    result[0] = new Byte[1];
                    result[0][0] = Byte.valueOf((byte) invalidTransfer);
                    result[1] = bytes;

                    // Write the result to the UI thread
                    Log.v("Ntag_I2C_Demo",":第三次：显示i2c数据");
                    publishProgress(result);
                }
            } catch (IOException e) {
                displayTransferDir(noTransfer);
                cancel(true);
                e.printStackTrace();
            } catch (FormatException e) {
                displayTransferDir(noTransfer);
                cancel(true);
                e.printStackTrace();
            } catch (InterruptedException e) {
                displayTransferDir(noTransfer);
                cancel(true);
                e.printStackTrace();
            } catch (TimeoutException e) {
                displayTransferDir(noTransfer);
                cancel(true);
                e.printStackTrace();
            } catch (CommandNotSupportedException e) {
                displayTransferDir(noTransfer);
                cancel(true);
                e.printStackTrace();
            }

            return null;
        }


        private void displayTransferDir(byte dir) {
            Byte[][] result;
            result = new Byte[2][0];
            result[0] = new Byte[1];
            result[0][0] = Byte.valueOf((byte) dir);
            publishProgress(result);
        }


        @Override
        public void onProgressUpdate(Byte[]... bytes) {
            super.onProgressUpdate(bytes);
            if (bytes[0][0] == noTransfer){
                main.setTransferDir("Transfer: non");
            } else if (bytes[0][0] == deviceToTag) {
                main.setTransferDir("Transfer: Device --> Tag");
            } else if (bytes[0][0] == tagToDevice) {
                main.setTransferDir("Transfer: Device <-- Tag");
            }

            String SensorResult = "";
            String SensorSingle;
            String StrVsource;
            String StrVdrv;
            int Vsource;
            int Vdrv;
            int tmph,tmpl;
            int SensorChn;
            double RefRes;
            double SensorRes;
            Log.v("Ntag_I2C_Demo",":length="+bytes[1].length);
            for(int i=0;i < bytes[1].length; i++){
                SensorSingle = bytes[1][i].toString();
                SensorResult = SensorResult.concat(SensorSingle);
                SensorResult = SensorResult.concat(",");
                Log.v("Ntag_I2C_Demo",":bytes[1]+["+i+"]="+bytes[1][i]);
                Log.v("Ntag_I2C_Demo",":SensorSingle="+SensorSingle);
                Log.v("Ntag_I2C_Demo",":SensorResult="+SensorResult);
            }
            if (bytes[1].length !=0 ) {
                if(bytes[1][0] < 0) {
                    tmph = bytes[1][0] + 256;
                }
                else {
                    tmph = bytes[1][0];
                }
                if(bytes[1][1] < 0) {
                    tmpl = bytes[1][1] + 256;
                }
                else {
                    tmpl = bytes[1][1];
                }
               Vsource = tmph*256+tmpl;
                if(bytes[1][2] < 0) {
                    tmph = bytes[1][2] + 256;
                }
                else {
                    tmph = bytes[1][2];
                }
                if(bytes[1][3] < 0) {
                    tmpl = bytes[1][3] + 256;
                }
                else {
                    tmpl = bytes[1][3];
                }
               Vdrv = tmph*256+tmpl;
                SensorChn = bytes[1][4];
                if(SensorChn == 4 ) {
                    RefRes = 100.0; //100K Ohm
                } else if(SensorChn == 3 ) {
                    RefRes = 10.0; //10K Ohm
                } else if(SensorChn == 2 ) {
                    RefRes = 1.0; //1K Ohm
                } else if(SensorChn == 1 ) {
                    RefRes = 0.1; //100 Ohm
                } else {
                    RefRes = 0.0f;
                }
                double tmp1 = (double)Vsource/(double)Vdrv;
                double tmp2 = 1/(tmp1 - 1.0);
                SensorRes = RefRes*tmp2;
                //SensorRes = RefRes/((Vsource/Vdrv)-1.0);
                //Vsource = Integer.parseInt(StrVsource);
                //Vdrv=Integer.parseInt(StrVdrv);
                //MainActivity.setSensorDisplay("Vsource="+Vsource+",Vdrv="+Vdrv);
                main.setSensorDisplay("1.times=" +times
                                + "\n"
                                +"2.The sensor raw data is:"
                                + "\n"
                                + SensorResult
                                + "\n"
                                + "3.ADC Data of Vsource="+Vsource
                                + "\n"
                                + "4.ADC Data of Vdrv="+Vdrv
                                + "\n"
                                + "5.SensorChn="+SensorChn
                                + "\n"
                                //+ ",tmp1="+ tmp1 + ",tmp2="+tmp2
                                + "6.Sensor Resistance Value="
                                + "\n"
                                + "   "+SensorRes+"KOhm"
                                ); //display 64 bytes i2c data
                float[] tmp = new float[4];
                tmp[0] = Vsource * (float)0.0078125;
                tmp[1] = Vdrv * (float)0.0078125;
                tmp[2] = SensorChn;
                //tmp[3] = (float) SensorRes;
                tmp[3] = tmp[0]-tmp[1];
                main.setDisplay(tmp);

//这里要怎么改才能求均值呢？或者改调用它的函数？
            }
//            else {
//                MainActivity.setSensorDisplay("The sensor data is NULL!");
//            }

        }
    }


    //stop the LED demo
    public void LEDFinish() {
        if (lTask != null && !lTask.isCancelled()) {
            lTask.exit = true;
            Log.v("Ntag_I2C_Demo"," step1");
            try {
                lTask.get();
                Log.v("Ntag_I2C_Demo"," step2");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            lTask = null;
        }
    }

    public boolean isReady() {
        if  (tag == null) { Log.v("Ntag_I2C_Demo"," : tag is null"); }
        if  (reader == null) { Log.v("Ntag_I2C_Demo"," : reader is null"); }
        if (tag != null && reader != null) {
            return true;
        }
        return false;
    }


    public int ObtainAuthStatus() {
        try {
            Ntag_Get_Version.Prod prod = reader.getProduct();
            if (!prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    && !prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
                return AuthActivity.AuthStatus.Disabled.getValue();
            } else {
                return reader.getProtectionPlus();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return AuthActivity.AuthStatus.Disabled.getValue();
    }

    public static boolean isTagPresent(Tag tag) {
        return true;
    }

    public void setBoardVersion() throws IOException, FormatException,
            CommandNotSupportedException {}

    public Boolean Flash(byte[] bytesToFlash) {
        return true;
    }

    public Boolean Auth(byte[] pwd, int authStatus) {
        try {
            if(authStatus == AuthActivity.AuthStatus.Unprotected.getValue()) {
                reader.protectPlus(pwd, Ntag_I2C_Commands.Register.Capability_Container.getValue());
            } else if(authStatus == AuthActivity.AuthStatus.Authenticated.getValue()) {
                reader.unprotectPlus();
            } else if(authStatus == AuthActivity.AuthStatus.Protected_W.getValue()
                    || authStatus == AuthActivity.AuthStatus.Protected_RW.getValue()
                    || authStatus == AuthActivity.AuthStatus.Protected_W_SRAM.getValue()
                    || authStatus == AuthActivity.AuthStatus.Protected_RW_SRAM.getValue()) {
                byte[] pack = reader.authenticatePlus(pwd);
                if(pack.length < 2) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (NotPlusTagException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Ntag_Get_Version.Prod getProduct() throws IOException {
        return reader.getProduct();
    }

    public byte[] readTagContent() {
        byte[] bytes = null;
        try {
            // The user memory and the first four pages are displayed
            int memSize = reader.getProduct().getMemsize() + 16;
            // Read all the pages using the fast read method
            bytes = reader.readEEPROM(0, memSize / reader.getBlockSize());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (CommandNotSupportedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public void readWriteConfigRegister() throws CommandNotSupportedException {
        // Check if the operation is read or write
        if (RegisterConfigActivity.isWriteChosen()) {
            try {
                Ntag_Get_Version.Prod prod = reader.getProduct();
                if((prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                        || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus))
                        && (RegisterConfigActivity.getAuth0() & 0xFF) <= 0xEB) {
                    showAuthWriteConfigAlert();
                } else {
                    writeConfigRegisters();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } //END if get chosen
        else {
            try {
                byte[] configRegisters = reader.getConfigRegisters();

                Ntag_I2C_Registers answer = getRegister_Settings(configRegisters);
                RegisterConfigActivity.setAnswer(answer, main);
                RegisterConfigActivity.setNC_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.NC_REG.getValue()]);
                RegisterConfigActivity.setLD_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.LAST_NDEF_PAGE.getValue()]);
                RegisterConfigActivity.setSM_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.SM_REG.getValue()]);
                RegisterConfigActivity.setNS_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.REG_LOCK.getValue()]);
                RegisterConfigActivity.setWD_LS_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.WDT_LS.getValue()]);
                RegisterConfigActivity.setWD_MS_Reg(configRegisters[I2C_Enabled_Commands.CR_Offset.WDT_MS.getValue()]);
                RegisterConfigActivity.setI2C_CLOCK_STR(configRegisters[I2C_Enabled_Commands.CR_Offset.I2C_CLOCK_STR.getValue()]);

                Ntag_Get_Version.Prod prod = reader.getProduct();
                if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                        || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
                    byte[] auth0Register = reader.getAuth0Register();
                    byte[] accessRegister = reader.getAccessRegister();
                    byte[] pti2cRegister = reader.getPTI2CRegister();

                    Ntag_I2C_Plus_Registers answerPlus = getPlusAuth_Settings(auth0Register, accessRegister, pti2cRegister);
                    RegisterConfigActivity.setAnswerPlus(answerPlus, main);
                    RegisterConfigActivity.setPlus_Auth0_Reg(auth0Register[3]);
                    RegisterConfigActivity.setPlus_Access_Reg(accessRegister[0]);
                    RegisterConfigActivity.setPlus_Pti2c_Reg(pti2cRegister[0]);
                }
                Toast.makeText(main, "read tag successfully done",
                        Toast.LENGTH_LONG).show();
            } catch (CommandNotSupportedException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(main, "read tag failed", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    /**
     *
     * Write config registers.
     *
     */
    private void writeConfigRegisters() {
        try {
            byte NC_Reg = (byte) RegisterConfigActivity.getNC_Reg();
            byte LD_Reg = (byte) RegisterConfigActivity.getLD_Reg();
            byte SM_Reg = (byte) RegisterConfigActivity.getSM_Reg();
            byte WD_LS_Reg = (byte) RegisterConfigActivity.getWD_LS_Reg();
            byte WD_MS_Reg = (byte) RegisterConfigActivity.getWD_MS_Reg();
            byte I2C_CLOCK_STR = (byte) RegisterConfigActivity.getI2C_CLOCK_STR();
            reader.writeConfigRegisters(NC_Reg, LD_Reg, SM_Reg, WD_LS_Reg, WD_MS_Reg, I2C_CLOCK_STR);
            Ntag_Get_Version.Prod prod = reader.getProduct();
            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                    || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
                byte AUTH0 = (byte) RegisterConfigActivity.getAuth0();
                byte ACCESS = (byte) RegisterConfigActivity.getAccess();
                byte PT_I2C = (byte) RegisterConfigActivity.getPTI2C();
                reader.writeAuthRegisters(AUTH0, ACCESS, PT_I2C);
            }
            Toast.makeText(main, "write tag successfully done",	Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(main, "write tag failed", Toast.LENGTH_LONG).show();
        }
    }

    private void showAuthWriteConfigAlert() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(main);
        builder.setTitle(main.getString(R.string.Dialog_enable_auth_title));
        builder.setMessage(main.getString(R.string.Dialog_enable_auth_msg));

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int index) {
                // Write config registers
                writeConfigRegisters();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
    }

    private Ntag_I2C_Registers getRegister_Settings(byte[] register)
            throws IOException, FormatException {
        Ntag_I2C_Registers answer = new Ntag_I2C_Registers();

        Ntag_Get_Version.Prod prod = reader.getProduct();

        if (!prod.equals(Ntag_Get_Version.Prod.Unknown)) {
            if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k)) {
                answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_1k);
            } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k)) {
                answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_2k);
            } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)) {
                answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_1k_Plus);
            } else if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
                answer.Manufacture = main.getString(R.string.ic_prod_ntagi2c_2k_Plus);
            }
            answer.Mem_size = prod.getMemsize();
        } else {
            answer.Manufacture = "";
            answer.Mem_size = 0;
        }

        byte NC_Reg = register[I2C_Enabled_Commands.SR_Offset.NC_REG.getValue()];

        // check I2C_RST_ON_OFF
        if ((NC_Reg & I2C_Enabled_Commands.NC_Reg_Func.I2C_RST_ON_OFF.getValue()) == I2C_Enabled_Commands.NC_Reg_Func.I2C_RST_ON_OFF
                .getValue()) {
            answer.I2C_RST_ON_OFF = true;
        } else {
            answer.I2C_RST_ON_OFF = false;
        }

        // check FD_OFF
        byte tmpReg = (byte) (NC_Reg & I2C_Enabled_Commands.NC_Reg_Func.FD_OFF.getValue());
        if (tmpReg == (0x30)) {
            answer.FD_OFF = main.getString(R.string.FD_OFF_ON_11);
        }
        if (tmpReg == (0x20)) {
            answer.FD_OFF = main.getString(R.string.FD_OFF_ON_10);
        }
        if (tmpReg == (0x10)) {
            answer.FD_OFF = main.getString(R.string.FD_OFF_ON_01);
        }
        if (tmpReg == (0x00)) {
            answer.FD_OFF = main.getString(R.string.FD_OFF_ON_00);
        }

        // check FD_ON
        tmpReg = (byte) (NC_Reg & I2C_Enabled_Commands.NC_Reg_Func.FD_ON.getValue());
        if (tmpReg == (0x0c)) {
            answer.FD_ON = main.getString(R.string.FD_OFF_ON_11);
        }
        if (tmpReg == (0x08)) {
            answer.FD_ON = main.getString(R.string.FD_OFF_ON_10);
        }
        if (tmpReg == (0x04)) {
            answer.FD_ON = main.getString(R.string.FD_OFF_ON_01);
        }
        if (tmpReg == (0x00)) {
            answer.FD_ON = main.getString(R.string.FD_OFF_ON_00);
        }

        // Last NDEF Page
        answer.LAST_NDEF_PAGE = (0x00000FF & register[I2C_Enabled_Commands.SR_Offset.LAST_NDEF_PAGE
                .getValue()]);

        byte NS_Reg = register[I2C_Enabled_Commands.SR_Offset.NS_REG.getValue()];

        // check NDEF_DATA_READ
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.NDEF_DATA_READ.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.NDEF_DATA_READ
                .getValue()) {
            answer.NDEF_DATA_READ = true;
        } else {
            answer.NDEF_DATA_READ = false;
        }

        // check RF_FIELD
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.RF_FIELD_PRESENT.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.RF_FIELD_PRESENT
                .getValue()) {
            answer.RF_FIELD_PRESENT = true;
        } else {
            answer.RF_FIELD_PRESENT = false;
        }

        // check PTHRU_ON_OFF
        if ((NC_Reg & (byte) I2C_Enabled_Commands.NC_Reg_Func.PTHRU_ON_OFF.getValue()) == I2C_Enabled_Commands.NC_Reg_Func.PTHRU_ON_OFF
                .getValue()) {
            answer.PTHRU_ON_OFF = true;
        } else {
            answer.PTHRU_ON_OFF = false;
        }

        // check I2C_LOCKED
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.I2C_LOCKED.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.I2C_LOCKED
                .getValue()) {
            answer.I2C_LOCKED = true;
        } else {
            answer.I2C_LOCKED = false;
        }

        // check RF_LOCK
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.RF_LOCKED.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.RF_LOCKED
                .getValue()) {
            answer.RF_LOCKED = true;
        } else {
            answer.RF_LOCKED = false;
        }

        // check check SRAM_I2C_Ready
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.SRAM_I2C_READY.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.SRAM_I2C_READY
                .getValue()) {
            answer.SRAM_I2C_READY = true;
        } else {
            answer.SRAM_I2C_READY = false;
        }

        // check SRAM_RF_READY
        tmpReg = (byte) (NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.SRAM_RF_READY.getValue());
        if ((NS_Reg & I2C_Enabled_Commands.NS_Reg_Func.SRAM_RF_READY.getValue()) == I2C_Enabled_Commands.NS_Reg_Func.SRAM_RF_READY
                .getValue()) {
            answer.SRAM_RF_READY = true;
        } else {
            answer.SRAM_RF_READY = false;
        }

        // check PTHRU_DIR
        tmpReg = (byte) (NC_Reg & (byte) 0x01);
        if (tmpReg == (0x01)) {
            answer.PTHRU_DIR = true;
        } else {
            answer.PTHRU_DIR = false;
        }

        // SM_Reg
        answer.SM_Reg = (0x00000FF & register[I2C_Enabled_Commands.SR_Offset.SM_REG.getValue()]);

        // WD_LS_Reg
        answer.WD_LS_Reg = (0x00000FF & register[I2C_Enabled_Commands.SR_Offset.WDT_LS.getValue()]);

        // WD_MS_Reg
        answer.WD_MS_Reg = (0x00000FF & register[I2C_Enabled_Commands.SR_Offset.WDT_MS.getValue()]);

        // check SRAM_MIRROR_ON_OFF
        if ((NC_Reg & I2C_Enabled_Commands.NC_Reg_Func.SRAM_MIRROR_ON_OFF.getValue()) == I2C_Enabled_Commands.NC_Reg_Func.SRAM_MIRROR_ON_OFF
                .getValue()) {
            answer.SRAM_MIRROR_ON_OFF = true;
        } else {
            answer.SRAM_MIRROR_ON_OFF = false;
        }

        // I2C_CLOCK_STR
        if (register[I2C_Enabled_Commands.SR_Offset.I2C_CLOCK_STR.getValue()] == 1) {
            answer.I2C_CLOCK_STR = true;
        } else {
            answer.I2C_CLOCK_STR = false;
        }

        // read NDEF Message
        try {
            NdefMessage message = reader.readNDEF();
            String NDEFText = new String(message.getRecords()[0].getPayload(),
                    "US-ASCII");
            NDEFText = NDEFText.subSequence(3, NDEFText.length()).toString();
            answer.NDEF_Message = NDEFText;
        } catch (Exception e) {
            e.printStackTrace();
            answer.NDEF_Message = main.getString(R.string.No_NDEF);
        }
        return answer;
    }

    private Ntag_I2C_Plus_Registers getPlusAuth_Settings(byte[] auth0register,
                                                         byte[] accessRegister,
                                                         byte[] pti2cRegister)
            throws IOException, FormatException {
        Ntag_I2C_Plus_Registers answerPlus = new Ntag_I2C_Plus_Registers();

        //Auth0 Register
        answerPlus.auth0 = (0x00000FF & auth0register[3]);

        //Access Register
        if ((0x0000080 & accessRegister[0]) >> I2C_Enabled_Commands.Access_Offset.NFC_PROT.getValue() == 1) {
            answerPlus.nfcProt = true;
        } else {
            answerPlus.nfcProt = false;
        }
        if ((0x0000020 & accessRegister[0]) >> I2C_Enabled_Commands.Access_Offset.NFC_DIS_SEC1.getValue() == 1) {
            answerPlus.nfcDisSec1 = true;
        } else {
            answerPlus.nfcDisSec1 = false;
        }
        answerPlus.authlim = (0x0000007 & accessRegister[0]);

        //PT I2C Register
        if ((0x0000008 & pti2cRegister[0]) >> I2C_Enabled_Commands.PT_I2C_Offset.K2_PROT.getValue() == 1) {
            answerPlus.k2Prot = true;
        } else {
            answerPlus.k2Prot = false;
        }
        if ((0x0000004 & pti2cRegister[0]) >> I2C_Enabled_Commands.PT_I2C_Offset.SRAM_PROT.getValue() == 1) {
            answerPlus.sram_prot = true;
        } else {
            answerPlus.sram_prot = false;
        }
        answerPlus.i2CProt = (0x0000003 & pti2cRegister[0]);
        return answerPlus;
    }

    public boolean isConnected() {
        return reader.isConnected();
    }

    public void readSessionRegisters() throws CommandNotSupportedException {

        try {
            byte[] sessionRegisters = reader.getSessionRegisters();
            Ntag_I2C_Registers answer = getRegister_Settings(sessionRegisters);
            RegisterSessionActivity.SetAnswer(answer, main);

            Toast.makeText(main, "read tag successfully done",
                    Toast.LENGTH_LONG).show();
        } catch (CommandNotSupportedException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(main, "read tag failed", Toast.LENGTH_LONG).show();
        }
    }

    public int resetTagMemory() {
        int bytesWritten = 0;

        try {
            bytesWritten = reader.writeDeliveryNdef();
        } catch (Exception e) {
            e.printStackTrace();
            bytesWritten = -1;
        }
        if(bytesWritten == 0) {
            //showDemoNotSupportedAlert();
        } else {
            byte NC_REG = (byte) 0x01;
            byte LD_Reg = (byte) 0x00;
            byte SM_Reg = (byte) 0xF8;
            byte WD_LS_Reg = (byte) 0x48;
            byte WD_MS_Reg = (byte) 0x08;
            byte I2C_CLOCK_STR = (byte) 0x01;
            // If we could reset the memory map, we should be able to write the config registers
            try {
                reader.writeConfigRegisters(NC_REG, LD_Reg,
                        SM_Reg, WD_LS_Reg, WD_MS_Reg, I2C_CLOCK_STR);
            } catch (Exception e) {
                //Toast.makeText(main, "Error writing configuration registers", Toast.LENGTH_LONG).show();
                e.printStackTrace();
                bytesWritten = -1;
            }

            try {
                Ntag_Get_Version.Prod prod = reader.getProduct();

                if (prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus)
                        || prod.equals(Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus)) {
                    byte AUTH0 = (byte) 0xFF;
                    byte ACCESS = (byte) 0x00;
                    byte PT_I2C = (byte) 0x00;
                    reader.writeAuthRegisters(AUTH0, ACCESS, PT_I2C);
                }
            } catch (Exception e) {
                //Toast.makeText(main, "Error writing authentication registers", Toast.LENGTH_LONG).show();
                e.printStackTrace();
                bytesWritten = -1;
            }
        }
        return bytesWritten;
    }



}
