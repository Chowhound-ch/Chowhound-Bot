package per.chowhound.bot.mirai.data.mongo.academic

import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import per.chowhound.bot.mirai.framework.components.academic.service.ClassMapService
import per.chowhound.bot.mirai.framework.components.academic.entity.ClassMap
import java.util.ArrayList

/**
 * @author zsck
 * @date   2022/11/8 - 15:44
 */
@Repository
interface ClassMapRepository: MongoRepository<ClassMap, Long>

@CacheConfig(cacheNames = ["classMap"])
@Service
class ClassMapServiceImpl(val repository: ClassMapRepository): ClassMapService {
    @CacheEvict(allEntries = true)
    override fun removeAll(): Long {
        return repository.count().also { repository.deleteAll() }
    }
    @Cacheable(key = "#root.methodName + #name")
    override fun likeClassName(name: String): List<ClassMap> {
        return repository.findAll(Example.of(ClassMap(className = name), ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)))
    }
    @Cacheable(key = "#root.methodName")
    override fun list(): List<ClassMap> {
        return repository.findAll()
    }
    @CacheEvict(allEntries = true)
    override fun saveBatch(classMapList: ArrayList<ClassMap>): Boolean {
        return repository.saveAll(classMapList).size == classMapList.size
    }
}