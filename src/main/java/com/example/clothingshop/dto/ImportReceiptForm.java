package com.example.clothingshop.dto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
public class ImportReceiptForm {
    private String note;
    private List<Line> lines = new ArrayList<>(List.of(new Line()));
    public String getNote(){return note;} public void setNote(String note){this.note=note;}
    public List<Line> getLines(){return lines;} public void setLines(List<Line> lines){this.lines=lines;}
    public static class Line {
        private Integer variantId; private Integer quantity; private BigDecimal importPrice;
        public Integer getVariantId(){return variantId;} public void setVariantId(Integer v){variantId=v;}
        public Integer getQuantity(){return quantity;} public void setQuantity(Integer q){quantity=q;}
        public BigDecimal getImportPrice(){return importPrice;} public void setImportPrice(BigDecimal p){importPrice=p;}
    }
}
