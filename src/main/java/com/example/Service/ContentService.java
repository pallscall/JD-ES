package com.example.Service;

import com.alibaba.fastjson.JSON;
import com.example.Pojo.Content;
import com.example.Utils.HtmlParseUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ContentService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 解析数据放入es索引
     * @param keyword 关键字
     * @return
     * @throws IOException
     */
    public Boolean parseContent(String keyword) throws IOException {
        ArrayList<Content> contents = new HtmlParseUtil().parseJD(keyword);

        //把查询到的数据放入 es中
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");

        for(int i = 0; i < contents.size(); i++){
            bulkRequest.add(
                    new IndexRequest("jd_goods")
                            .source(JSON.toJSONString(contents.get(i)), XContentType.JSON));
        }

        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !bulk.hasFailures();
    }

    /**
     * 搜索功能
     * @param keyword  搜索关键字
     * @param pageNo   分页起始
     * @param pageSize 分页大小
     * @return
     * @throws IOException
     */
    public List<Map<String,Object>> searchPage(String keyword, int pageNo, int pageSize) throws IOException {
        if(pageNo <= 1) pageNo = 1;

        //条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //分页
        searchSourceBuilder.from(pageNo);
        searchSourceBuilder.size(pageSize);

        //精准匹配
        TermQueryBuilder title = QueryBuilders.termQuery("title", keyword);
        searchSourceBuilder.query(title);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        //执行搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //解析结果
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit sh:searchResponse.getHits().getHits()){
            list.add(sh.getSourceAsMap());
        }
        return list;
    }

    /**
     *
     * @param keyword
     * @param pageNo
     * @param pageSize
     * @return
     * @throws IOException
     */
    public List<Map<String,Object>> searchPageHighLight(String keyword, int pageNo, int pageSize) throws IOException {
        if(pageNo <= 1) pageNo = 1;

        //条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //分页
        searchSourceBuilder.from(pageNo);
        searchSourceBuilder.size(pageSize);

        //精准匹配
        TermQueryBuilder title = QueryBuilders.termQuery("title", keyword);
        searchSourceBuilder.query(title);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.requireFieldMatch(false);  //一个字段中多个高亮只高亮一个
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //执行搜索
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //解析结果
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        for(SearchHit sh:searchResponse.getHits().getHits()){

            //获取高亮字段
            Map<String, HighlightField> highlightFields = sh.getHighlightFields();
            HighlightField hf = highlightFields.get("title");
            Map<String, Object> sourceAsMap = sh.getSourceAsMap();  //原来的结果
            //解析高亮字段
            if(hf!=null){
                Text[] fragments = hf.fragments();  //拿到高亮字段
                String newTitle = "";
                for(Text text:fragments){
                    newTitle += text;
                }
                sourceAsMap.put("title",newTitle);
            }
            list.add(sourceAsMap);

        }
        return list;
    }
}

