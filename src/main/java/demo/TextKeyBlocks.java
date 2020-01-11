package demo;

import org.opencv.core.Rect;

public class TextKeyBlocks {
    public final Rect listRect;
    public final int goodsColumnWidth;

    public TextKeyBlocks(Rect listRect, int goodsColumnWidth) {
        this.listRect = listRect;
        this.goodsColumnWidth = goodsColumnWidth;
    }
}
