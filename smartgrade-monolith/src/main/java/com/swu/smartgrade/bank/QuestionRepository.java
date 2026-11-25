package com.swu.smartgrade.bank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByKnowledgeTag(String knowledgeTag);

    @Query("""
            select q from Question q
            where (:subject is null or q.subject = :subject)
              and (:knowledgeTag is null or q.knowledgeTag = :knowledgeTag)
              and (:keyword is null or lower(q.stem) like lower(concat('%', :keyword, '%')))
            order by q.id
            """)
    List<Question> search(@Param("subject") String subject,
                          @Param("knowledgeTag") String knowledgeTag,
                          @Param("keyword") String keyword);

    List<Question> findByKnowledgeTagIn(List<String> knowledgeTags);
}
