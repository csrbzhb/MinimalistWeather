package com.baronzhang.android.weather.feature.home.drawer;

import com.baronzhang.android.weather.di.component.ApplicationComponent;
import com.baronzhang.android.weather.di.scope.ActivityScoped;
import com.baronzhang.android.weather.feature.home.MainActivity;

import dagger.Component;

/**
 * Created by Administrator on 2018/1/9 0009.
 */

@ActivityScoped
@Component(modules = DrawerMenuModule.class, dependencies = ApplicationComponent.class)

public interface DrawerMenuComponent {

    void inject1(DrawerMenuFragment mainActivity);
}
