package com.application.common.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public class ExcelUtil {
    public static Integer getNullableIntegerCell(Row row, int cellIndex){
        Cell cell = row.getCell(cellIndex);
        if(cell != null && cell.getCellType() == CellType.NUMERIC){
            return (int) cell.getNumericCellValue();
        }

        return null;
    }

    public static Double getNullableDoubleCell(Row row, int cellIndex){
        Cell cell = row.getCell(cellIndex);
        if(cell != null && cell.getCellType() == CellType.NUMERIC){
            return cell.getNumericCellValue();
        }
        return null;
    }

    public static String getNullableStringCell(Row row, int cellIndex){
        Cell cell = row.getCell(cellIndex);
        if(cell != null && cell.getCellType() == CellType.STRING){
            return cell.getStringCellValue();
        }

        return null;
    }
}
