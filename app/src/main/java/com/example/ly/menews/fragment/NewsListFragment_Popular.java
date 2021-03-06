package com.example.ly.menews.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.ly.menews.R;
import com.example.ly.menews.activity.NewsContentActivity;
import com.example.ly.menews.adapter.NewsAdapter;
import com.example.ly.menews.domain.News;
import com.example.ly.menews.utils.CommonRequest;
import com.example.ly.menews.utils.CommonResponse;
import com.example.ly.menews.utils.Consts;
import com.example.ly.menews.utils.HttpUtil;
import com.example.ly.menews.utils.UserManager;
import com.example.ly.menews.utils.Util;
import com.google.gson.Gson;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * 热点新闻
 */
public class NewsListFragment_Popular extends Fragment {

    private PullToRefreshListView mListView;
    private List<News> newsList = new ArrayList<>();

    //新闻list适配器
    private NewsAdapter newsAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_news_list_fragment__popular, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        initData();
    }


    //初始化控件
    private void initView() {

        View view = getView();
        mListView = view.findViewById(R.id.listview_popular);
        newsAdapter=new NewsAdapter(getContext(),R.layout.news_item,newsList);

    }


    private void initData() {

        //初始化datas
        getNewsDatas();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setAdapter(newsAdapter);
            }
        },3000);



        mListView.setMode(PullToRefreshBase.Mode.BOTH);

        mListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> pullToRefreshBase) {
                Toast.makeText(getContext(), "下拉刷新", Toast.LENGTH_SHORT).show();
                //initData();
                getNewsDatas();
                mListView.postDelayed(new Runnable() {
                    public void run() {

                        //停止刷新
                        mListView.onRefreshComplete();
                    }
                },1000);

            }
        });

        mListView.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
            @Override
            public void onLastItemVisible() {
                new DownloadNews().execute(Consts.URL_GetNews,Consts.REQUEST_NEWS_POPULAR);
                Toast.makeText(getContext(), "上拉加载", Toast.LENGTH_SHORT).show();

            }
        });


        //添加点击监听
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String title=newsList.get(position).getTitle();
                // 查找本地数据库中是否已存在当前用户,不存在则新建用户并写入
                // User user = LitePal.where("username=?",account).findFirst(User.class);
                News news= LitePal.where("title=?",title).findFirst(News.class);
                //跳转到新闻内容活动中，并把新闻url传过去
                if(news!=null){
                    Intent intent = new Intent(getActivity(), NewsContentActivity.class);
                    intent.putExtra("url",news.getUrl());
                    startActivity(intent);

                    //在这里将新闻关键字添加到用户的兴趣列表
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }

            }
        });


    }




    /**
     * 初始化datas（这里通常是网络请求获取初始数据）
     */
    public void getNewsDatas() {

        /**
         * 传入异步下载参数
         * Consts.URL_GetNews：下载请求url
         * Consts.REQUEST_NEWS_CF:请求码：001：代表请求推荐新闻
         */
        new DownloadNews().execute(Consts.URL_GetNews,Consts.REQUEST_NEWS_POPULAR);
    }



    class DownloadNews extends AsyncTask<String,Void,Void> {

        public  final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        private OkHttpClient client = new OkHttpClient();

        public DownloadNews(){
            super();
        }

        @Override
        protected Void doInBackground(String... strings) {

            //创建请求体对象
            CommonRequest request = new CommonRequest();
            // 填充参数:账号和请求码
            request.addRequestParam("account", UserManager.getCurrentUser().getAccount());
            request.addRequestParam("requestCode",strings[1]);
            // POST请求
            HttpUtil.sendPost(strings[0], request.getJsonStr(), new okhttp3.Callback() {

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    CommonResponse res = new CommonResponse(response.body().string());
                    //将新闻list转换成news数组
                    News[] newsArray=new Gson().fromJson(res.getJSONArray(),News[].class);
                    for(News news:newsArray){
                        newsList.add(news);
                        news.save();
                    }

                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    e.printStackTrace();
                }
            });
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            newsAdapter.notifyDataSetChanged();
        }
    }




}
