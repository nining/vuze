/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AEThread2;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class AnimatedImage {
	
	//wait time in ms
	private static final int SPEED = 100;
	
	Canvas canvas;
	boolean running;
	
	private AEThread2 runner;
	
	private Image[] images;
	private int currentImage = 0;

	private String imageName;
	
	public AnimatedImage(Composite parent) {
		canvas = new Canvas(parent,SWT.NONE);
		Color background = null;
		Composite p = parent;
		while(p != null && background == null) {
			background = p.getBackground();
			if(background != null) {
				System.out.println("background : " + background + ", composite : " + p);
			}
			p = p.getParent();
		}
		
		canvas.setBackground(background);
		canvas.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				stop();
				disposeImages();
			}
		});
	}
	
	
	private void renderNextImage() {
		if(!canvas.isDisposed()) {
			Display display = canvas.getDisplay();
			if(!display.isDisposed()) {
				display.asyncExec( new Runnable() {
					public void run() {
						if(!canvas.isDisposed() && images != null) {
							currentImage++;
							if(currentImage >= images.length) {
								currentImage = 0;
							}
							if(currentImage < images.length) {
								Image image = images[currentImage];
								if(image != null && !image.isDisposed()) {
									GC gc = new GC(canvas);
									Point canvasSize = canvas.getSize();
									Rectangle imageBounds = image.getBounds();
									
									gc.drawImage(image, (canvasSize.x-imageBounds.width)/2, (canvasSize.y-imageBounds.height)/2);
									gc.dispose();
								}
							}
						}
					}
				});
			}
		}
	}
	
	public void setLayoutData(Object data) {
		canvas.setLayoutData(data);
	}
	
	public void start() {
		running = true;
		runner = new AEThread2("image runner",true) {
			public void run() {
				while(running) {
					try {
						renderNextImage();
						Thread.sleep(SPEED);
					} catch (Exception e) {
						running = false;
					}
				}
			}
		};
		runner.start();
	}
	
	public void stop() {
		running = false;
	}
	
	public Control getControl() {
		return canvas;
	}
	
	public void dispose() {
		if(canvas != null && !canvas.isDisposed()) {
			canvas.dispose();
		}
	}
	
	public void setImageFromName(String imageName) {
		this.imageName = imageName;
		ImageLoader imageLoader = ImageLoader.getInstance();
		images = imageLoader.getImages(imageName);
	}
	
	private void setImages(Image[] images) {
		disposeImages();
		this.images = images;
	}

	private void disposeImages() {
		if(images != null) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			imageLoader.releaseImage(imageName);
			images = null;
		}
	}

}
