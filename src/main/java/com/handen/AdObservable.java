package com.handen;

import io.reactivex.Observable;
import io.reactivex.Observer;

class AdObservable<T> extends Observable {

    private Boolean isGooglePlayVertical;

    @Override
    protected void subscribeActual(Observer observer) {
    }

    public boolean isGooglePlayVertical() {
        return isGooglePlayVertical;
    }

    public void setGooglePlayVertical(Boolean googlePlayVertical) {
        isGooglePlayVertical = googlePlayVertical;
    }
}
