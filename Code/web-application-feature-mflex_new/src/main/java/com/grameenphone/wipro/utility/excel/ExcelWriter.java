package com.grameenphone.wipro.utility.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Function;

public abstract class ExcelWriter<T> {
    private Workbook workbook = new XSSFWorkbook();
    private Sheet sheet = workbook.createSheet();
    private ArrayList<SheetData> fields = new ArrayList();
    private String defaultDatePattern;
    private Function<Object, String> defaultFormatter;

    private final static Logger logger = LoggerFactory.getLogger(ExcelWriter.class);

    public final static int DEFAULT_COLUMN_WIDTH = 25;

    private class SheetData<U> {
        public String id;
        public SheetColumn column;
        public Function<Object, String> formatter;
        public Function<U, Object> value;

        public SheetData(String id, SheetColumn column, ColumnFormatter formatter, Function<U, Object> field) throws IOException {
            this.id = id;
            this.column = column;
            this.value = field;
            if(formatter != null) {
                try {
                    this.formatter = formatter.formatter().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IOException("Incorrect Formatter Definition");
                }
            }
        }
    }

    private static abstract class SheetColumnInst implements SheetColumn {
        private SheetColumn originalColumn;
        public SheetColumnInst() {}
        public SheetColumnInst(SheetColumn originalColumn) {this.originalColumn = originalColumn;}
        @Override
        public Class<? extends Annotation> annotationType() {
            return SheetColumn.class;
        }

        @Override
        public int width() {
            return originalColumn == null ? 0 : originalColumn.width();
        }

        @Override
        public int order() {
            return originalColumn == null ? 0 : originalColumn.order();
        }

        @Override
        public String datePattern() {
            return originalColumn == null ? "" : originalColumn.datePattern();
        }
    }

    {
        sheet.setDefaultColumnWidth(DEFAULT_COLUMN_WIDTH);
    }

    public ExcelWriter() throws IOException {
        Class<T> modelClass = (Class<T>) ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        SheetColumn defaultColumn = modelClass.getAnnotation(SheetColumn.class);
        if(defaultColumn != null) {
            if(defaultColumn.width() != 0) {
                sheet.setDefaultColumnWidth(defaultColumn.width());
            }
            if(!"".equals(defaultColumn.datePattern())) {
                defaultDatePattern = defaultColumn.datePattern();
            }
        }
        ColumnFormatter defaultFormatter = modelClass.getAnnotation(ColumnFormatter.class);
        if(defaultFormatter != null) {
            try {
                this.defaultFormatter = defaultFormatter.formatter().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IOException("Incorrect Formatter Definition");
            }
        }
        for (Field declaredField : modelClass.getDeclaredFields()) {
            SheetColumn column = declaredField.getAnnotation(SheetColumn.class);
            ColumnFormatter formatter = declaredField.getAnnotation(ColumnFormatter.class);
            if(column != null) {
                if("".equals(column.label())) {
                    column = new SheetColumnInst(column) {
                        @Override
                        public String label() {
                            return declaredField.getName();
                        }
                    };
                }
                fields.add(new SheetData<T>(declaredField.getName(), column, formatter, (d) -> {
                    try {
                        return declaredField.get(d);
                    } catch (IllegalAccessException e) {
                        logger.debug("Couldn't read field value", e);
                        return null;
                    }
                }));
            }
        }
        Collections.sort(fields, (a, b) -> {
            SheetColumn aColumn = a.column;
            SheetColumn bColumn = b.column;
            if(aColumn.order() > bColumn.order()) {
                return 1;
            } else if(aColumn.order() == bColumn.order()) {
                return 0;
            }
            return -1;
        });
    }

    public ExcelWriter(int headerCount) throws IOException {
        int i = 0;
        for (; i < headerCount; i++) {
            String header = "" + i;
            SheetColumn column = new SheetColumnInst() {
                @Override
                public String label() {
                    return header;
                }
            };
            int index = i;
            fields.add(new SheetData<List<Object>>(header, column, null, (data) -> data.get(index)));
        }
    }

    public ExcelWriter(List<String> headers) throws IOException {
        int i = 0;
        for (String header : headers) {
            SheetColumn column = new SheetColumnInst() {
                @Override
                public String label() {
                    return header;
                }
            };
            int index = i++;
            fields.add(new SheetData<List<Object>>("" + index, column, null, (data) -> data.get(index)));
        }
    }

    public ExcelWriter(Map<String, String> headers) throws IOException {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            SheetColumn column = new SheetColumnInst() {
                @Override
                public String label() {
                    return header.getValue();
                }
            };
            fields.add(new SheetData<Map<String, Object>>(header.getKey(), column, null, (data) -> data.get(header.getKey())));
        }
    }

    public void setDefaultColumnWidth(int width) {
        sheet.setDefaultColumnWidth(width);
    }

    public void setDefaultDatePattern(String pattern) {
        defaultDatePattern = pattern;
    }

    public void setDefaultFormatter(Function<Object, String> formatter) {
        defaultFormatter = formatter;
    }

    public void setFormatter(String field, Function<Object, String> formatter) {
        fields.forEach((f) -> {
            if(field.equals(f.id)) {
                f.formatter = formatter;
            }
        });
    }

    public void addHeader() {
        Row row = sheet.createRow(0);
        int[] cellCount = new int[] {0};
        fields.forEach((f) -> {
            SheetColumn fColumn = f.column;
            String text = fColumn.label();
            Cell cell = row.createCell(cellCount[0]++, CellType.STRING);
            CellStyle style = workbook.createCellStyle();
            ((XSSFCellStyle)style).setFillForegroundColor(new XSSFColor(new Color(210, 210, 210)));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            cell.setCellStyle(style);
            cell.setCellValue(text);
        });
    }

    public void write(List<T> data) {
        int[] cellCount = new int[] {0};
        //set Width of table grids
        fields.forEach((f) -> {
            SheetColumn fColumn = f.column;
            cellCount[0]++;
            if(fColumn.width() != 0) {
                sheet.setColumnWidth(cellCount[0] - 1, fColumn.width() * 256);
            }
        });

        int[] rowCount = new int[] {0};
        data.forEach(d -> {
            Row row = sheet.createRow(++rowCount[0]);
            cellCount[0] = 0;
            fields.forEach((f) -> {
                Object value = f.value.apply(d);
                cellCount[0]++;
                if(value != null) {
                    String text = serialize(f, value);
                    Cell cell = row.createCell(cellCount[0] - 1, CellType.STRING);
                    cell.setCellValue(text);
                }
            });
        });
    }

    private String serialize(SheetData column, Object v) {
        if(defaultFormatter != null) {
            return defaultFormatter.apply(v);
        }
        if(column.formatter != null) {
            return ((Function<Object, String>)column.formatter).apply(v);
        }
        if(v instanceof Date) {
            return new SimpleDateFormat("".equals(column.column.datePattern()) ? defaultDatePattern == null ? "dd-MM-yyyy" : defaultDatePattern : column.column.datePattern()).format((Date) v);
        }
        if(v instanceof Double || v instanceof Float) {
            return new DecimalFormat("#.##").format(v);
        }
        return v.toString();
    }

    public void flush(OutputStream data) throws IOException {
        workbook.write(data);
        workbook.close();
    }
}