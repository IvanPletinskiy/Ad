package com.handen;

import io.reactivex.Observable;
import io.reactivex.Observer;

class AdObservable<T> extends Observable {

//    private Boolean isGooglePlayVertical;
    private boolean isAdPreviouslyClicked;

    public AdObservable() {
        this.isAdPreviouslyClicked = false;
    }

    @Override
    protected void subscribeActual(Observer observer) {
    }
/*
    public Boolean isGooglePlayVertical() {
        return isGooglePlayVertical;
    }
*/
/*
    public void setGooglePlayVertical(Boolean googlePlayVertical) {
        isGooglePlayVertical = googlePlayVertical;
    }
*/
    public boolean isAdPreviouslyClicked() {
        return isAdPreviouslyClicked;
    }

    public void setAdPreviouslyClicked(boolean adPreviouslyClicked) {
        isAdPreviouslyClicked = adPreviouslyClicked;
    }
}
