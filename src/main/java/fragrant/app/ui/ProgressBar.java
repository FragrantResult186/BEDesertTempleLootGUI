package fragrant.app.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

public class ProgressBar extends BasicProgressBarUI {
    private int animationPosition = 0;

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        super.paintDeterminate(g, c);

        JProgressBar progressBar = (JProgressBar) c;

        // 進捗が100%未満のみ表示
        if (progressBar.getValue() < 100) {
            int width = progressBar.getWidth();   // 横幅
            int height = progressBar.getHeight(); // 高さ
            int ANIMATION_WIDTH = 5; // アニメーションバーの幅（%）
            int barWidth = width * ANIMATION_WIDTH / 100;
            int xPos = (animationPosition * (width - barWidth)) / 100; // アニメーションバーのX座標（左端からの位置）

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(progressBar.getForeground().brighter());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)); // 透明度
            g2d.fillRect(xPos, 1, barWidth, height - 2);
            g2d.dispose();
        }
    }

    /**
     * 不確定状態の描画
     */
    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c);
    }

    /**
     * アニメーション位置を更新
     *
     * @param step アニメーション位置をどれだけ進めるか
     */
    public void updateAnimationPosition(int step) {
        animationPosition += step;
        if (animationPosition > 100) {
            animationPosition = 0; // 右端を超えたら左端に戻す
        }
    }

    /**
     * アニメーション位置指定
     *
     * @param position 位置（0～100）
     */
    public void setAnimationPosition(int position) {
        this.animationPosition = position;
    }

}
