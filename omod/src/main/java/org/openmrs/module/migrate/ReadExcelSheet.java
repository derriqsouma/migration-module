package org.openmrs.module.migrate;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by derric on 7/14/14.
 */
public class ReadExcelSheet {

    public ReadExcelSheet(){
    }

    public List<List<Object>> readExcelSheet(String path) throws Exception {
        List<List<Object>> sheetData = new ArrayList();

        FileInputStream file = new FileInputStream(new File(path));


        HSSFWorkbook workbook = new HSSFWorkbook(file);

        HSSFSheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {

            Row row = rowIterator.next();
            if (row.getRowNum() == 0 || row.getRowNum() == 1 || row.getRowNum() == 2) {
                continue;
            }

            int expectedColumns = 24;
            List<Object> rowData = new ArrayList();

            for (int i = 0; i < expectedColumns; i++) {
                Cell cell = row.getCell(i);

                if (cell == null) {
                    rowData.add("");
                    continue;
                }

                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_BOOLEAN:
                        rowData.add(cell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        rowData.add(cell.getNumericCellValue());
                        break;
                    case Cell.CELL_TYPE_STRING:
                        rowData.add(cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_BLANK:
                        rowData.add(cell.getStringCellValue());
                        break;
                }
            }
            sheetData.add(rowData);
        }
        file.close();

        return sheetData;
    }
}
