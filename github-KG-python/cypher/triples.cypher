// 有模型的 且 有论文 且 有依赖且依赖数大于8的
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

// 接力式： 正常多跳关系
// Paper - PAPER_INTRODUCE_MODEL -> Model （非主干）
MATCH (subject:Paper)<-[predicate:MODEL_INTRODUCED_IN_PAPER]-(object:Model)
RETURN DISTINCT 'Paper_' + subject.paperTitle AS subject, 'PAPER_INTRODUCE_MODEL' AS predicate, 'Model_' + object.name AS object
  ORDER BY subject, object

// Model - MODEL_ON_DATASET -> Dataset （主干。。）
MATCH (subject:Model)-[predicate:MODEL_ON_DATASET]->(object:Dataset)
RETURN DISTINCT 'Model_' + subject.name AS subject, 'MODEL_ON_DATASET' AS predicate, 'Dataset_' + object.name AS object
  ORDER BY subject, object

// Task - TASK_HAS_PAPER -> Paper
MATCH (subject:Task)-[predicate:TASK_HAS_PAPER]->(object:Paper)
RETURN DISTINCT 'Task_' + subject.name AS subject, 'TASK_HAS_PAPER' AS predicate, 'Paper_' + object.paperTitle AS object
  ORDER BY subject, object

// Task - TASK_HAS_DATASET -> Dataset （非主干。。）
MATCH (subject:Task)-[predicate:TASK_HAS_DATASET]->(object:Dataset)
RETURN DISTINCT 'Task_' + subject.name AS subject, 'TASK_HAS_DATASET' AS predicate, 'Dataset_' + object.name AS object
  ORDER BY subject, object

// Category - CATEGORY_HAS_TASK -> Task
MATCH (subject:Category)-[:CATEGORY_HAS_TASK]->(object:Task)
RETURN DISTINCT 'Category_' + subject.name AS subject, 'CATEGORY_HAS_TASK' AS predicate, 'Task_' + object.name AS object
  ORDER BY subject, object

// Task - TASK_HAS_SUBTASK -> Subtask
MATCH (subject:Task)<-[:TASK_HAS_SUBTASK]-(object:Task)
RETURN DISTINCT 'Task_' + subject.name AS subject, 'TASK_HAS_SUBTASK' AS predicate, 'Task_' + object.name AS object
  ORDER BY subject, object

// Paper - PAPER_USES_METHOD - Method
MATCH (subject:Paper)-[:PAPER_USES_METHOD]->(object:Method)
RETURN DISTINCT 'Paper_' + subject.paperTitle AS subject, 'PAPER_USES_METHOD' AS predicate, 'Method_' + object.name AS object
  ORDER BY subject, object

// Collection - COLLECTION_HAS_METHOD -> Method
MATCH (subject:Collection)-[:COLLECTION_HAS_METHOD]->(object:Method)
RETURN DISTINCT 'Collection_' + subject.name AS subject, 'COLLECTION_HAS_METHOD' AS predicate, 'Method_' + object.name AS object
  ORDER BY subject, object

// Area - AREA_HAS_COLLECTION -> Collection
MATCH (subject:Area)-[:AREA_HAS_COLLECTION]->(object:Collection)
RETURN DISTINCT 'Area_' + subject.area AS subject, 'AREA_HAS_COLLECTION' AS predicate, 'Collection_' + object.name AS object
  ORDER BY subject, object

// 压缩式： 这种是完全以 repo为中心 多跳 路径压缩
// Task
// Repository - REPO_IMPLEMENTS_PAPER - PAPER - TASK_HAS_PAPER - Task
MATCH (subject:Repository)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper)<-[:TASK_HAS_PAPER]-(object:Task)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_UNDER_TASK' AS predicate, 'Task_' + object.name AS object
  ORDER BY subject, object
// Method
// Repository - REPO_IMPLEMENTS_PAPER - Paper - PAPER_USES_METHOD - Method
MATCH (subject:Repository)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper)-[:PAPER_USES_METHOD]->(object:Method)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_USES_METHOD' AS predicate, 'Method_' + object.name AS object
  ORDER BY subject, object
// Dataset
// 【错误】 TASK_HAS_DATASET 不能表示 REPO_USES_DATASET 关系
/*Repository - REPO_IMPLEMENTS_PAPER - PAPER - TASK_HAS_PAPER - Task - TASK_HAS_DATASET - Dataset
MATCH (subject:Repository)<-[:PAPER_IMPLEMENTED_BY_REPO]-(:Paper)<-[:TASK_HAS_PAPER]-(:Task)-[:TASK_HAS_DATASET]->(object:Dataset)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_USES_DATASET' AS predicate, 'Dataset_' + object.name AS object
  ORDER BY subject, object*/
// Repository - REPO_IMPLEMENTS_MODEL - Model - MODEL_ON_DATASET -> Dataset
MATCH (subject:Repository)<-[:MODEL_IMPLEMENTED_BY_REPO]-(:Model)-[:MODEL_ON_DATASET]->(object:Dataset)
RETURN DISTINCT 'Repository_' + subject.nameWithOwner AS subject, 'REPO_USES_DATASET' AS predicate, 'Dataset_' + object.name AS object
  ORDER BY subject, object
// 像 category 还勉强

// Collection 、 Area 这样的高阶关系就很难用一个压缩的关系概括

// 和 Category 的关系
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