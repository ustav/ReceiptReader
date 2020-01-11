package demo;

import org.opencv.core.Rect;

public class TextKeyBlocks {
    public final Rect listRect;
    public final int goodsColumnWidth;
    public final String total;
    public final String date;

    public TextKeyBlocks(Rect listRect, int goodsColumnWidth, String total, String date) {
        this.listRect = listRect;
        this.goodsColumnWidth = goodsColumnWidth;
        this.total = total;
        this.date = date;
    }
}
