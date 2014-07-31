package org.openmrs.module.migrate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by derric on 7/31/14.
 */
public class ConvertStringToDate {

    /*constructor*/
    public ConvertStringToDate(){
    }

    public Date convert(String dateInString) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        Date date = null;
        if (dateInString != "") {
            if (dateInString.contains("/")) {

                date = formatter.parse(dateInString);
            }

        }
        return date;
    }
}
