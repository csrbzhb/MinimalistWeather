package com.baronzhang.android.weather.feature.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baronzhang.android.weather.AppConstants;
import com.baronzhang.android.weather.base.BaseFragment;
import com.baronzhang.android.weather.R;
import com.baronzhang.android.weather.data.db.entities.minimalist.AirQualityLive;
import com.baronzhang.android.weather.data.db.entities.minimalist.WeatherForecast;
import com.baronzhang.android.weather.data.db.entities.minimalist.LifeIndex;
import com.baronzhang.android.weather.data.db.entities.minimalist.Weather;
import com.baronzhang.android.weather.data.WeatherDetail;
import com.baronzhang.android.widget.IndicatorView;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.comm.util.AdError;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HomePageFragment extends BaseFragment implements HomePageContract.View, NativeExpressAD.NativeExpressADListener {
    private static final String TAG = "HomePageFragment";
    //AQI
    @BindView(R.id.tv_aqi)
    TextView aqiTextView;
    @BindView(R.id.tv_quality)
    TextView qualityTextView;
    @BindView(R.id.indicator_view_aqi)
    IndicatorView aqiIndicatorView;
    @BindView(R.id.tv_advice)
    TextView adviceTextView;
    @BindView(R.id.tv_city_rank)
    TextView cityRankTextView;

    //详细天气信息
    @BindView(R.id.detail_recycler_view)
    RecyclerView detailRecyclerView;

    //预报
    @BindView(R.id.forecast_recycler_view)
    RecyclerView forecastRecyclerView;

    //生活指数
    @BindView(R.id.life_index_recycler_view)
    RecyclerView lifeIndexRecyclerView;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private Unbinder unbinder;

    private Weather weather;

    private List<WeatherDetail> weatherDetails;
    private List<WeatherForecast> weatherForecasts;
    private List<LifeIndex> lifeIndices;

    private DetailAdapter detailAdapter;
    private ForecastAdapter forecastAdapter;
    private LifeIndexAdapter lifeIndexAdapter;

    private HomePageContract.Presenter presenter;
    private ViewGroup adcontainer;
    private NativeExpressAD nativeExpressAD;
    private NativeExpressADView nativeExpressADView;
    private int adWidth, adHeight; // 广告宽高
    private boolean isAdFullWidth = true, isAdAutoHeight =true; // 是否采用了ADSize.FULL_WIDTH，ADSize.AUTO_HEIGHT

    public HomePageFragment() {

    }

    public static HomePageFragment newInstance() {

        return new HomePageFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            onFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home_page, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        adcontainer = (ViewGroup) rootView.findViewById(R.id.fl_container);
        //天气详情
        detailRecyclerView.setNestedScrollingEnabled(false);
        detailRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        weatherDetails = new ArrayList<>();
        detailAdapter = new DetailAdapter(weatherDetails);
        detailAdapter.setOnItemClickListener((adapterView, view, i, l) -> {
        });
        forecastRecyclerView.setItemAnimator(new DefaultItemAnimator());
        detailRecyclerView.setAdapter(detailAdapter);

        //天气预报
        forecastRecyclerView.setNestedScrollingEnabled(false);
        forecastRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        weatherForecasts = new ArrayList<>();
        forecastAdapter = new ForecastAdapter(weatherForecasts);
        forecastAdapter.setOnItemClickListener((adapterView, view, i, l) -> {
        });
        forecastRecyclerView.setItemAnimator(new DefaultItemAnimator());
        forecastRecyclerView.setAdapter(forecastAdapter);

        //生活指数
        lifeIndexRecyclerView.setNestedScrollingEnabled(false);
        lifeIndexRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        lifeIndices = new ArrayList<>();
        lifeIndexAdapter = new LifeIndexAdapter(getActivity(), lifeIndices);
        lifeIndexAdapter.setOnItemClickListener((adapterView, view, i, l) -> Toast.makeText(HomePageFragment.this.getContext(), lifeIndices.get(i).getDetails(), Toast.LENGTH_LONG).show());
        lifeIndexRecyclerView.setItemAnimator(new DefaultItemAnimator());
        lifeIndexRecyclerView.setAdapter(lifeIndexAdapter);

        aqiIndicatorView.setIndicatorValueChangeListener((currentIndicatorValue, stateDescription, indicatorTextColor) -> {
            aqiTextView.setText(String.valueOf(currentIndicatorValue));
            if (TextUtils.isEmpty(weather.getAirQualityLive().getQuality())) {
                qualityTextView.setText(stateDescription);
            } else {
                qualityTextView.setText(weather.getAirQualityLive().getQuality());
            }
            aqiTextView.setTextColor(indicatorTextColor);
            qualityTextView.setTextColor(indicatorTextColor);
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        assert presenter != null;
        presenter.subscribe();
        refreshAd();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void displayWeatherInformation(Weather weather) {

        this.weather = weather;
        onFragmentInteractionListener.updatePageTitle(weather);

        AirQualityLive airQualityLive = weather.getAirQualityLive();
        aqiIndicatorView.setIndicatorValue(airQualityLive.getAqi());
        adviceTextView.setText(airQualityLive.getAdvice());
        String rank = airQualityLive.getCityRank();
        cityRankTextView.setText(TextUtils.isEmpty(rank) ? "首要污染物: " + airQualityLive.getPrimary() : rank);

        weatherDetails.clear();
        weatherDetails.addAll(createDetails(weather));
        detailAdapter.notifyDataSetChanged();

        weatherForecasts.clear();
        weatherForecasts.addAll(weather.getWeatherForecasts());
        forecastAdapter.notifyDataSetChanged();

        lifeIndices.clear();
        lifeIndices.addAll(weather.getLifeIndexes());
        lifeIndexAdapter.notifyDataSetChanged();

        onFragmentInteractionListener.addOrUpdateCityListInDrawerMenu(weather);
    }

    private List<WeatherDetail> createDetails(Weather weather) {

        List<WeatherDetail> details = new ArrayList<>();
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "体感温度", weather.getWeatherLive().getFeelsTemperature() + "°C"));
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "湿度", weather.getWeatherLive().getHumidity() + "%"));
//        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "气压", (int) Double.parseDouble(weather.getWeatherLive().getAirPressure()) + "hPa"));
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "紫外线指数", weather.getWeatherForecasts().get(0).getUv()));
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "降水量", weather.getWeatherLive().getRain() + "mm"));
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "降水概率", weather.getWeatherForecasts().get(0).getPop() + "%"));
        details.add(new WeatherDetail(R.drawable.ic_index_sunscreen, "能见度", weather.getWeatherForecasts().get(0).getVisibility() + "km"));
        return details;
    }

    @Override
    public void setPresenter(HomePageContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unSubscribe();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public interface OnFragmentInteractionListener {
        void updatePageTitle(Weather weather);

        /**
         * 更新完天气数据同时需要刷新侧边栏的已添加的城市列表
         *
         * @param weather 天气数据
         */
        void addOrUpdateCityListInDrawerMenu(Weather weather);
    }

    private void refreshAd() {
        try {
            /**
             *  如果选择支持视频的模版样式，请使用{@link Constants#NativeExpressSupportVideoPosID}
             */
            nativeExpressAD = new NativeExpressAD(getContext(), getMyADSize(), AppConstants.APPID, AppConstants.NativePosID, this); // 这里的Context必须为Activity
            nativeExpressAD.setVideoOption(new VideoOption.Builder()
                    .setAutoPlayPolicy(VideoOption.AutoPlayPolicy.WIFI) // 设置什么网络环境下可以自动播放视频
                    .setAutoPlayMuted(true) // 设置自动播放视频时，是否静音
                    .build()); // setVideoOption是可选的，开发者可根据需要选择是否配置
            nativeExpressAD.loadAD(1);
        } catch (NumberFormatException e) {
            Log.w(TAG, "ad size invalid.");
            Toast.makeText(getContext(), "请输入合法的宽高数值", Toast.LENGTH_SHORT).show();
        }
    }

    private ADSize getMyADSize() {
        int w = isAdFullWidth ? ADSize.FULL_WIDTH : adWidth;
        int h = isAdAutoHeight ? ADSize.AUTO_HEIGHT : adHeight;
        return new ADSize(w, h);
    }


    @Override
    public void onNoAD(AdError adError) {
        Log.i(
                TAG,
                String.format("onNoAD, error code: %d, error msg: %s", adError.getErrorCode(),
                        adError.getErrorMsg()));
    }

    @Override
    public void onADLoaded(List<NativeExpressADView> adList) {
        Log.i(TAG, "onADLoaded: " + adList.size());
        // 释放前一个展示的NativeExpressADView的资源
        if (nativeExpressADView != null) {
            nativeExpressADView.destroy();
        }

        if (adcontainer.getVisibility() != View.VISIBLE) {
            adcontainer.setVisibility(View.VISIBLE);
        }

        if (adcontainer.getChildCount() > 0) {
            adcontainer.removeAllViews();
        }

        nativeExpressADView = adList.get(0);
        // 广告可见才会产生曝光，否则将无法产生收益。
        adcontainer.addView(nativeExpressADView);
        nativeExpressADView.render();
    }

    @Override
    public void onRenderFail(NativeExpressADView adView) {
        Log.i(TAG, "onRenderFail");
    }

    @Override
    public void onRenderSuccess(NativeExpressADView adView) {
        Log.i(TAG, "onRenderSuccess");
    }

    @Override
    public void onADExposure(NativeExpressADView adView) {
        Log.i(TAG, "onADExposure");
    }

    @Override
    public void onADClicked(NativeExpressADView adView) {
        Log.i(TAG, "onADClicked");
    }

    @Override
    public void onADClosed(NativeExpressADView adView) {
        Log.i(TAG, "onADClosed");
        // 当广告模板中的关闭按钮被点击时，广告将不再展示。NativeExpressADView也会被Destroy，释放资源，不可以再用来展示。
        if (adcontainer != null && adcontainer.getChildCount() > 0) {
            adcontainer.removeAllViews();
            adcontainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onADLeftApplication(NativeExpressADView adView) {
        Log.i(TAG, "onADLeftApplication");
    }

    @Override
    public void onADOpenOverlay(NativeExpressADView adView) {
        Log.i(TAG, "onADOpenOverlay");
    }

    @Override
    public void onADCloseOverlay(NativeExpressADView adView) {
        Log.i(TAG, "onADCloseOverlay");
    }

    /**
     * 注意：带有视频的广告被点击后会进入全屏播放视频，此时视频可以跟随屏幕方向的旋转而旋转，
     * 请开发者注意处理好自己的Activity的运行时变更，不要让Activity销毁。
     * 例如，在AndroidManifest文件中给Activity添加属性android:configChanges="keyboard|keyboardHidden|orientation|screenSize"，
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
