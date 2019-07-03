package bakar.labyrinta;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Bakar on 10.03.2018.
 */

public class CustomTouchListener implements View.OnTouchListener {

    private GameRenderer gameRenderer;

    private MotionEvent.PointerCoords first_single_touch = new MotionEvent.PointerCoords();
    private MotionEvent.PointerCoords first_multi_touch = new MotionEvent.PointerCoords();;
    private MotionEvent.PointerCoords second_multi_touch = new MotionEvent.PointerCoords();;

    private float minimumOffsetModule = 10.f; // FIXME: 05.05.2018 сделать зависимость от размеров экрана
    private boolean offsedAlreadyChanged = false;

    public void setRenderer(GameRenderer renderer){
        gameRenderer = renderer;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent){

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: // нажатие
                motionEvent.getPointerCoords(0, first_single_touch);
                gameRenderer.onTouchDown(first_single_touch);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                gameRenderer.onTouchUp(null); // при 2 нажатиях меняется только скейл
                motionEvent.getPointerCoords(0, first_multi_touch);
                motionEvent.getPointerCoords(1, second_multi_touch);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
                offsedAlreadyChanged = false;
                gameRenderer.onTouchUp(new PointF(first_single_touch.x, first_single_touch.y));
                break;
            case MotionEvent.ACTION_MOVE: // движение

                if (motionEvent.getPointerCount() > 1){
                    MotionEvent.PointerCoords current_touch = new MotionEvent.PointerCoords();
                    MotionEvent.PointerCoords current_touch2 = new MotionEvent.PointerCoords();
                    motionEvent.getPointerCoords(0, current_touch);
                    motionEvent.getPointerCoords(1, current_touch2);
                    PointF current_pt = new PointF(current_touch.x, current_touch.y);
                    PointF current_pt2 = new PointF(current_touch2.x, current_touch2.y);
                    PointF first_pt = new PointF(first_multi_touch.x, first_multi_touch.y);
                    PointF first_pt2 = new PointF(second_multi_touch.x, second_multi_touch.y);

                    PointF first_dif = new PointF(first_pt.x - first_pt2.x, first_pt.y - first_pt2.y);
                    PointF current_dif = new PointF(current_pt.x - current_pt2.x, current_pt.y - current_pt2.y);

                    float first_l = (float)Math.sqrt((first_dif.x * first_dif.x + first_dif.y * first_dif.y));
                    float current_l = (float)Math.sqrt((current_dif.x * current_dif.x + current_dif.y * current_dif.y));

                    gameRenderer.changeScale(first_l - current_l);

                    motionEvent.getPointerCoords(0, first_multi_touch);
                    motionEvent.getPointerCoords(1, second_multi_touch);
                }
                if (motionEvent.getPointerCount() == 1){
                    MotionEvent.PointerCoords current_touch = new MotionEvent.PointerCoords();
                    motionEvent.getPointerCoords(0, current_touch);
                    PointF current_pt = new PointF(current_touch.x, current_touch.y);
                    PointF first_pt = new PointF(first_single_touch.x, first_single_touch.y);
                    if (gameRenderer.isMovingOffset){
                        PointF offset = new PointF(first_pt.x - current_pt.x, first_pt.y - current_pt.y);
                        if (offset.length() > minimumOffsetModule || offsedAlreadyChanged){
                            gameRenderer.changeOffset(offset);
                            offsedAlreadyChanged = true;
                        }
                    }
                    if(gameRenderer.isMovingPlayer){
                        gameRenderer.movePlayer(current_pt);
                    }
                    motionEvent.getPointerCoords(0, first_single_touch);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}
