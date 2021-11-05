//WHERE exists((subject)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper))
//AND exists((subject)<-[:MODEL_IMPLEMENTED_BY_REPO]-(:Model))
//AND exists((subject)-[:REPO_DEPENDS_ON_PACKAGE]->(:Package))
//AND subject.packageDegree > 8

// Repository - REPO_USES_LANGUAGE -> Language
MATCH (subject:Repository)-[predicate:REPO_USES_LANGUAGE]->(object:Language)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_USES_LANGUAGE' AS predicate, 'Language_' + object.name AS object
  ORDER BY subject, object

// Repository - REPO_DEPENDS_ON_PACKAGE -> Package
MATCH (subject:Repository)-[predicate:REPO_DEPENDS_ON_PACKAGE]->(object:Package)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_DEPENDS_ON_PACKAGE' AS predicate, 'Package_' + object.nameWithManager AS object
  ORDER BY subject, object

// Repository - PackageDegree
MATCH (subject:Repository)
OPTIONAL MATCH (subject)-[predicate:REPO_DEPENDS_ON_PACKAGE]->(object:Package)
WITH 'Repository_' + subject.nameWithOwner AS subject, count(object) AS PackageDegree
RETURN subject, PackageDegree
  ORDER BY subject
/*MATCH (subject:Repository)
RETURN 'Repository_' + subject.nameWithOwner AS repo, subject.packageDegree AS packageDegree
  ORDER BY repo*/

// Repository - REPO_BELONGS_TO_OWNER -> Owner
MATCH (subject:Repository)-[predicate:REPO_BELONGS_TO_OWNER]->(object:Owner)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_BELONGS_TO_OWNER' AS predicate, 'Owner_' + object.login AS object
  ORDER BY subject, object

// Repository - REPO_UNDER_TOPIC -> Topic
MATCH (subject:Repository)-[predicate:REPO_UNDER_TOPIC]->(object:Topic)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_UNDER_TOPIC' AS predicate, 'Topic_' + object.name AS object
  ORDER BY subject, object

// Repository - REPO_IMPLEMENTS_MODEL - Model
MATCH (subject:Repository)<-[predicate:MODEL_IMPLEMENTED_BY_REPO]-(object:Model)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_IMPLEMENTS_MODEL' AS predicate, 'Model_' + object.name AS object
  ORDER BY subject, object

// Repository - REPO_IMPLEMENTS_PAPER - Paper
MATCH (subject:Repository)<-[predicate:PAPER_IMPLEMENTED_BY_REPO]-(object:Paper)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_IMPLEMENTS_PAPER' AS predicate, 'Paper_' + object.paperTitle AS object
  ORDER BY subject, object



//和 Category 的关系
//1 evaluation-tables.json 以 Model和Repo是完整的，加上 2 links-between-papers-and-code.json Paper和Repo 的补充，总体达成
// 加上与model平级的paper改进后，repo应该只需要关联 paper 就是全部了
// Repository - REPO_IMPLEMENTS_MODEL - Model - TASK_HAS_MODEL - Task - CATEGORY_HAS_TASK - Category
MATCH (subject:Repository)<-[:MODEL_IMPLEMENTED_BY_REPO]-(:Model)<-[:TASK_HAS_MODEL]-(:Task)
        <-[:CATEGORY_HAS_TASK]-(object:Category)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject,
                'REPO_UNDER_CATEGORY' AS predicate,
                'Category_' + object.name AS object
  ORDER BY subject, object
UNION
// Repository - REPO_IMPLEMENTS_MODEL - Model - TASK_HAS_MODEL - (sub)Task - TASK_HAS_SUBTASK - Task - CATEGORY_HAS_TASK - Category
MATCH (subject:Repository)<-[:MODEL_IMPLEMENTED_BY_REPO]-(:Model)<-[:TASK_HAS_MODEL]-(:Task)<-[:TASK_HAS_SUBTASK]-(:Task)
        <-[:CATEGORY_HAS_TASK]-(object:Category)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject,
                'REPO_UNDER_CATEGORY' AS predicate,
                'Category_' + object.name AS object
  ORDER BY subject, object
UNION
// Repository - REPO_IMPLEMENTS_PAPER - PAPER - TASK_HAS_PAPER - Task - CATEGORY_HAS_TASK - Category
MATCH (subject:Repository)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper)<-[:TASK_HAS_PAPER]-(:Task)
        <-[:CATEGORY_HAS_TASK]-(object:Category)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject,
                'REPO_UNDER_CATEGORY' AS predicate,
                'Category_' + object.name AS object
  ORDER BY subject, object
UNION
// Repository - REPO_IMPLEMENTS_PAPER - Paper - TASK_HAS_PAPER - (sub)Task - TASK_HAS_SUBTASK - Task - CATEGORY_HAS_TASK - Category
MATCH (subject:Repository)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper)<-[:TASK_HAS_PAPER]-(:Task)<-[:TASK_HAS_SUBTASK]-(:Task)
        <-[:CATEGORY_HAS_TASK]-(object:Category)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject,
                'REPO_UNDER_CATEGORY' AS predicate,
                'Category_' + object.name AS object
  ORDER BY subject, object