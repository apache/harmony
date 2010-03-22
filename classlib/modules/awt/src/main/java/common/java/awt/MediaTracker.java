/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Igor V. Stolyarov
 */
package java.awt;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class MediaTracker implements Serializable {

    private static final long serialVersionUID = -483174189758638095L;

    public static final int ABORTED = 2;

    public static final int COMPLETE = 8;

    public static final int ERRORED = 4;

    public static final int LOADING = 1;

    Component owner;
    LinkedList<TrackingImage> trackingObjects = new LinkedList<TrackingImage>();

    public MediaTracker(Component comp) {
        owner = comp;
    }

    public void addImage(Image image, int id) {
        addImage(image, id, -1, -1);
    }

    public synchronized void addImage(Image image, int id, int w, int h) {
        int idx = 0;
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(id < node.getID()){
                idx = li.previousIndex();
                break;
            }
            idx = li.nextIndex();
        }
        trackingObjects.add(idx, new TrackingImage(this, image, id, w, h));
    }

    public boolean checkAll() {
        return checkAll(false);
    }

    public boolean checkAll(boolean load) {
        boolean result = true;
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(load) {
                node.loadMedia();
            }
            if((node.getState() & TrackingNode.LOADING_DONE) == 0){
                result = false;
            }
        }
        return result;
    }

    public boolean checkID(int id) {
        return checkID(id, false);
    }

    public boolean checkID(int id, boolean load) {
        boolean result = true;
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node.getID() == id){
                if(load) {
                    node.loadMedia();
                }
                if((node.getState() & TrackingNode.LOADING_DONE) == 0){
                    result = false;
                }
            }
        }
        return result;
    }

    public synchronized Object[] getErrorsAny() {
        ArrayList<Object> errors = new ArrayList<Object>();
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if((node.getState() & ERRORED) != 0){
                errors.add(node.getMediaObject());
            }
        }
        if(errors.size() == 0) {
            return null;
        }
        return errors.toArray();
    }

    public synchronized Object[] getErrorsID(int id) {
        ArrayList<Object> errors = new ArrayList<Object>();
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node.getID() == id){
                if((node.getState() & ERRORED) != 0){
                    errors.add(node.getMediaObject());
                }
            }
        }
        if(errors.size() == 0) {
            return null;
        }
        return errors.toArray();
    }

    public synchronized boolean isErrorAny() {
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if((node.getState() & ERRORED) != 0) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isErrorID(int id) {
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node.getID() == id){
                if((node.getState() & ERRORED) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void removeImage(Image image) {
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node.getMediaObject() == image) {
                li.remove();
            }
        }
    }

    public synchronized void removeImage(Image image, int id) {
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node.getID() == id && node.getMediaObject() == image) {
                li.remove();
            }
        }
    }

    public synchronized void removeImage(Image image, int id, int width,
            int height) {
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(node instanceof TrackingImage){
                TrackingImage ti = (TrackingImage) node;
                if(ti.equals(image, id, width, height)) {
                    li.remove();
                }
            }
        }
    }

    public int statusAll(boolean load) {
        int status = 0;
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(load) {
                node.loadMedia();
            }
            status |= node.getState();
        }
        return status;
    }

    public int statusID(int id, boolean load) {
        int status = 0;
        ListIterator<TrackingImage> li = trackingObjects.listIterator();
        while(li.hasNext()){
            TrackingNode node = li.next();
            if(id == node.getID()){
                if(load) {
                    node.loadMedia();
                }
                status |= node.getState();
            }
        }
        return status;
    }

    public void waitForAll() throws InterruptedException {
        waitForAll(0);
    }

    public synchronized boolean waitForAll(long ms) throws InterruptedException {
        boolean needLoad = true;
        long finishtime = System.currentTimeMillis() + ms;
        while(true){
            int status = statusAll(needLoad);
            if((status & LOADING) == 0){
                return (status == COMPLETE);
            }
            needLoad = false;
            long timeout;
            if(ms == 0){
                timeout = 0;
            }else{
                long curtime = System.currentTimeMillis();
                if(finishtime <= curtime) {
                    return false;
                }
                timeout = finishtime - curtime;
            }
            wait(timeout);
        }
    }

    public void waitForID(int id) throws InterruptedException {
        waitForID(id , 0);
    }

    public synchronized boolean waitForID(int id, long ms)
            throws InterruptedException {
        boolean needLoad = true;
        long finishtime = System.currentTimeMillis() + ms;
        while(true){
            int status = statusID(id, needLoad);
            if((status & LOADING) == 0){
                return (status == COMPLETE);
            }
            needLoad = false;
            long timeout;
            if(ms == 0){
                timeout = 0;
            }else{
                long curtime = System.currentTimeMillis();
                if(finishtime <= curtime) {
                    return false;
                }
                timeout = finishtime - curtime;
            }
            wait(timeout);
        }
    }

    synchronized void refresh(){
        notifyAll();
    }

    abstract class TrackingNode{
        final static int LOADING_DONE = ABORTED | ERRORED | COMPLETE;
        final static int LOADING_STARTED = LOADING | ERRORED | COMPLETE;

        MediaTracker tracker;
        int id;
        int state;
        TrackingNode next;


        TrackingNode(MediaTracker tracker, int id){
            this.tracker = tracker;
            this.id = id;
        }

        abstract Object getMediaObject();
        abstract void loadMedia();
        abstract int getState();

        int getID(){
            return id;
        }

        void updateState(int state){
            synchronized(this){
                this.state = state;
            }
            tracker.refresh();
        }

    }

    class TrackingImage extends TrackingNode implements ImageObserver{
        Image img;
        int w, h;

        TrackingImage(MediaTracker tracker, Image image, int id, int w, int h) {
            super(tracker, id);
            img = image;
            this.w = w;
            this.h = h;
        }

        @Override
        Object getMediaObject() {
            return img;
        }

        @Override
        void loadMedia() {
            if((state & LOADING_STARTED) == 0){
                state = (state & ~ABORTED) | LOADING;
                if(tracker.owner.prepareImage(img, w, h, this)){
                    updateState(COMPLETE);
                }
            }
        }

        @Override
        synchronized int getState(){
            int infoflags = tracker.owner.checkImage(img, w, h, this);
            int st = translateFlags(infoflags);
            if(st != 0 && st != state) {
                updateState(st);
            }
            return state;
        }

        int translateFlags(int infoflags){
            if((infoflags & ERROR) != 0) {
                return ERRORED;
            } else if((infoflags & ABORT) != 0) {
                return ABORTED;
            } else if((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
                return COMPLETE;
            } else {
                return 0;
            }
        }

        boolean equals(Image image, int id, int width, int height){
            return (image == img && this.id == id && w == width && h == height);
        }

        public boolean imageUpdate(Image image, int infoflags, int x, int y, int w, int h) {
            int st = translateFlags(infoflags);
            if(st != 0 && st != state) {
                updateState(st);
            }
            return ((state & LOADING) != 0);
        }

    }

}

