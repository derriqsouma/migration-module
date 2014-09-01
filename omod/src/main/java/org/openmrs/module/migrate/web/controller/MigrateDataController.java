package org.openmrs.module.migrate.web.controller;

/**
 * Created by derric on 6/20/14.
 */

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.Iterator;

/**
 * The main controller.
 */
@Controller
public class MigrateDataController {

    @RequestMapping(value = "/module/migrate/migrateData", method = RequestMethod.GET)
    public void manage(ModelMap model) {

    }

    @RequestMapping(value = "/module/migrate/migrateData", method = RequestMethod.POST)
    public void submitInput(ModelMap model, @RequestParam(value = "file", required = true) String file) {
        if (file != null) {
            String path = "/home/derric/Desktop/ampath_data/MOH_361A/" + file;

            model.addAttribute("thefile", file);

            readExcelSheet(path);
        } else {
            String error = "NO FILE HAS BEEN SELECTED";
            model.addAttribute("error", error);
        }
    }

    public static void readExcelSheet(String path) {

        try {

            FileInputStream file = new FileInputStream(new File(path));

            //Get the workbook instance for XLS file
            HSSFWorkbook workbook = new HSSFWorkbook(file);

            //Get first sheet from the workbook
            HSSFSheet sheet = workbook.getSheetAt(0);

            //Iterate through each rows from first sheet
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                //For each row, iterate through each columns
                Iterator<org.apache.poi.ss.usermodel.Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_BOOLEAN:
                            System.out.print(cell.getBooleanCellValue() + "\t");

                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            System.out.print(cell.getNumericCellValue() + "\t");

                            break;
                        case Cell.CELL_TYPE_STRING:
                            System.out.print(cell.getStringCellValue() + "\t");

                            break;
                    }

                }
                System.out.println(" \n****\n");
            }

            file.close();
//            FileOutputStream out = new FileOutputStream(new File("/home/derric/Desktop/test1.xls"));
//            workbook.write(out);
//            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
