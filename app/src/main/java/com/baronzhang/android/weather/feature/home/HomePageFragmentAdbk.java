package com.baronzhang.android.weather.feature.home;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.baronzhang.android.weather.AppConstants;
import com.baronzhang.android.weather.R;
import com.baronzhang.android.weather.base.BaseFragment;
import com.baronzhang.android.weather.data.WeatherDetail;
import com.baronzhang.android.weather.data.db.entities.minimalist.AirQualityLive;
import com.baronzhang.android.weather.data.db.entities.minimalist.LifeIndex;
import com.baronzhang.android.weather.data.db.entities.minimalist.Weather;
import com.baronzhang.android.weather.data.db.entities.minimalist.WeatherForecast;
import com.baronzhang.android.widget.IndicatorView;
import com.qq.e.ads.nativ.NativeAD;
import com.qq.e.ads.nativ.NativeADDataRef;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;
import com.qq.e.comm.util.GDTLogger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HomePageFragmentAdbk extends BaseFragment implements HomePageContract.View,NativeAD.NativeAdListener {

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
    protected AQuery $;
    private NativeADDataRef adItem;
    private NativeAD nativeAD;
    View rootView;
    public HomePageFragmentAdbk() {

    }

    public static HomePageFragmentAdbk newInstance() {

        return new HomePageFragmentAdbk();
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
        rootView = inflater.inflate(R.layout.fragment_home_page, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        $ = new AQuery(getContext());
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
        lifeIndexAdapter.setOnItemClickListener((adapterView, view, i, l) -> Toast.makeText(HomePageFragmentAdbk.this.getContext(), lifeIndices.get(i).getDetails(), Toast.LENGTH_LONG).show());
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
        loadAD();
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


    public void loadAD() {
        if (nativeAD == null) {
            this.nativeAD = new NativeAD(getContext(), AppConstants.APPID, AppConstants.NativePosID, this);
        }
        int count = 1; // 一次拉取的广告条数：范围1-10
        nativeAD.loadAD(count);
    }


    /**
     * 展示原生广告时，一定要先调用onExposured接口曝光广告，否则将无法调用onClicked点击接口
     */
    public void showAD() {
        if (adItem.getAdPatternType() == AdPatternType.NATIVE_3IMAGE) {
            GDTLogger.d("show three img ad.");
            rootView.findViewById(R.id.native_3img_ad_container).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.native_ad_container).setVisibility(View.INVISIBLE);
            $.id(R.id.img_1).image(adItem.getImgList().get(0), false, true);
            $.id(R.id.img_2).image(adItem.getImgList().get(1), false, true);
            $.id(R.id.img_3).image(adItem.getImgList().get(2), false, true);
            $.id(R.id.native_3img_title).text((String) adItem.getTitle());
            $.id(R.id.native_3img_desc).text((String) adItem.getDesc());
        } else if (adItem.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT) {
            GDTLogger.d("show two img ad.");
            rootView.findViewById(R.id.native_3img_ad_container).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.native_ad_container).setVisibility(View.VISIBLE);
            $.id(R.id.img_logo).image((String) adItem.getIconUrl(), false, true);
            $.id(R.id.img_poster).image(adItem.getImgUrl(), false, true);
            $.id(R.id.text_name).text((String) adItem.getTitle());
            $.id(R.id.text_desc).text((String) adItem.getDesc());
        }
        $.id(R.id.btn_download).text(getADButtonText());
        adItem.onExposured(this. rootView.findViewById(R.id.nativeADContainer)); // 需要先调用曝光接口
        $.id(R.id.btn_download).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adItem.onClicked(view); // 点击接口
            }
        });
    }

    /**
     * App类广告安装、下载状态的更新（普链广告没有此状态，其值为-1） 返回的AppStatus含义如下： 0：未下载 1：已安装 2：已安装旧版本 4：下载中（可获取下载进度“0-100”）
     * 8：下载完成 16：下载失败
     */
    private String getADButtonText() {
        if (adItem == null) {
            return "……";
        }
        if (!adItem.isAPP()) {
            return "查看详情";
        }
        switch (adItem.getAPPStatus()) {
            case 0:
                return "点击下载";
            case 1:
                return "点击启动";
            case 2:
                return "点击更新";
            case 4:
                return adItem.getProgress() > 0 ? "下载中" + adItem.getProgress()+ "%" : "下载中"; // 特别注意：当进度小于0时，不要使用进度来渲染界面
            case 8:
                return "下载完成";
            case 16:
                return "下载失败,点击重试";
            default:
                return "查看详情";
        }
    }


    @Override
    public void onADLoaded(List<NativeADDataRef> arg0) {
        if (arg0.size() > 0) {
            adItem = arg0.get(0);
            showAD();
            Toast.makeText(getContext(), "原生广告加载成功", Toast.LENGTH_LONG).show();
        } else {
            Log.i("AD_DEMO", "NOADReturn");
        }
    }

    @Override
    public void onADStatusChanged(NativeADDataRef arg0) {
        $.id(R.id.btn_download).text(getADButtonText());
    }

    @Override
    public void onADError(NativeADDataRef adData, AdError error) {
        Log.i(
                "AD_DEMO",
                String.format("onADError, error code: %d, error msg: %s", error.getErrorCode(),
                        error.getErrorMsg()));
    }

    @Override
    public void onNoAD(AdError error) {
        Log.i(
                "AD_DEMO",
                String.format("onNoAD, error code: %d, error msg: %s", error.getErrorCode(),
                        error.getErrorMsg()));
    }

}
