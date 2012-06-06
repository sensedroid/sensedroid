package dk.itu.noxdroid.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

public class Line extends View {
	private Paint paint = new Paint();
	private float[] points;
	
	public Line(Context context, float[] points){
		super(context);
		
		paint.setColor(Color.rgb(80, 80, 80));
		paint.setAntiAlias(true);
		paint.setStrokeWidth(15.0f);
		LinearGradient rg =  new LinearGradient(points[0], points[1], points[2], points[3], Color.rgb(110, 110, 100), Color.rgb(220, 220, 220), Shader.TileMode.REPEAT);		
		paint.setShader(rg);
		this.points = points;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawLine(points[0], points[1], points[2], points[3], paint);
	}

}
