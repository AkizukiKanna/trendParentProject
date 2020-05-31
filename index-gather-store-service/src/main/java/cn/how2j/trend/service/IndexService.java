package cn.how2j.trend.service;
 
import cn.how2j.trend.pojo.Index;
import cn.how2j.trend.util.SpringContextUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
 
@Service
@CacheConfig(cacheNames="indexes")
public class IndexService {
    private List<Index> indexes;
//    private Index[] indexes;
    @Autowired RestTemplate restTemplate;

    //如果fetch_indexes_from_third_part获取失败了，就自动调用 third_part_not_connected 并返回
    //HystrixCommand默认1s获取不到数据就执行fallbackMethod
//    @HystrixCommand(fallbackMethod = "third_part_not_connected")
//    @Cacheable(key="'all_codes'")
    public List<Index> fetch_indexes_from_third_part(){
        List<Map> temp= restTemplate.getForObject("http://127.0.0.1:8090/indexes/codes.json",List.class);
//        Index[] temp= restTemplate.getForObject("http://127.0.0.1:8090/indexes/codes.json",Index[].class);
        return map2Index(temp);
//        return temp;
    }

    public List<Index> third_part_not_connected(){
        System.out.println("third_part_not_connected()");
        Index index= new Index();
        index.setCode("000000");
        index.setName("无效指数代码");
        return CollectionUtil.toList(index);
//        Index[] indexs=new Index[]{index};
//        return indexs;
    }
 
    private List<Index> map2Index(List<Map> temp) {
        List<Index> indexes = new ArrayList<>();
        for (Map map : temp) {
            String code = map.get("code").toString();
            String name = map.get("name").toString();
            Index index= new Index();
            index.setCode(code);
            index.setName(name);
            indexes.add(index);
        }
 
        return indexes;
    }

    @CacheEvict(allEntries=true)
    public void remove(){
    }

    @Cacheable(key="'all_codes'")
    public List<Index> get(){
        return CollUtil.toList();
    }

    @Cacheable(key="'all_codes'")
    public List<Index> store(){
        return indexes;
    }

    @HystrixCommand(fallbackMethod = "third_part_not_connected")
    public List<Index> fresh() {
        indexes =fetch_indexes_from_third_part();
        System.out.println("没有执行断路器");
        IndexService indexService = SpringContextUtil.getBean(IndexService.class);
        indexService.remove();
        return indexService.store();
    }
}