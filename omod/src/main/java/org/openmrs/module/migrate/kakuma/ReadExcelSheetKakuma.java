package org.openmrs.module.migrate.kakuma;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.module.kenyaui.KenyaUiUtils;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by derric on 2/12/15.
 */
public class ReadExcelSheetKakuma {
    HttpSession session;
    KenyaUiUtils kenyaUi;
    String path;

    public ReadExcelSheetKakuma(String path, HttpSession session, KenyaUiUtils kenyaUi) {
        this.session = session;
        this.kenyaUi = kenyaUi;
        this.path = path;
    }


    public List<List<Object>> readExcelSheet() {
        List<List<Object>> sheetData = new ArrayList();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        try {
            FileInputStream file = new FileInputStream(new File(path));

            HSSFWorkbook workbook = new HSSFWorkbook(file);

            HSSFSheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();
                if (row.getRowNum() == 0 ) {
                    continue;
                }

                int expectedColumns = 34;
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
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(format.format(cell.getDateCellValue()));
                            } else {
                                rowData.add(cell.getNumericCellValue());
                            }
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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            kenyaUi.notifyError(session, "No such file or directory");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sheetData;
    }
}
