// 1 evaluation-tables.json

// Task - TASK_UNDER_CATEGORY -> Category
// 好像还不能这么一步到位，因为无法保证不重复
//MERGE (task:Task {name: $task})-[under:UNDER_CATEGORY]-(category:CATEGORY {name: $category})

// Task
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.description = $description

// Category - CATEGORY_HAS_TASK -> Task
MATCH (task:Task {name: $taskName})
MERGE (category:Category {name: $category})
  ON CREATE SET category.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET category.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (category)-[has:CATEGORY_HAS_TASK]->(task)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Task - TASK_HAS_DATASET -> Dataset
MATCH (task:Task {name: $taskName})
MERGE (dataset:Dataset {name: $datasetName})
  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dataset.description = $description
MERGE (task)-[has_dataset:TASK_HAS_DATASET]->(dataset)
  ON CREATE SET has_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Task - TASK_HAS_MODEL -> Model
MATCH (task:Task {name: $taskName})
MERGE (model:Model {name: $modelName})
  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task)-[task_has_model:TASK_HAS_MODEL]->(model)
  ON CREATE SET task_has_model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task_has_model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Model - MODEL_ON_DATASET -> Dataset
MATCH (dataset:Dataset {name: $datasetName})
MERGE (model:Model {name: $modelName})
//把metric作为 Dataset & Model之间属性 最后有几个metric就插入几条关系
MERGE (model)-[model_on_dataset:MODEL_ON_DATASET {metricName: $metricName}]->(dataset)
  ON CREATE SET model_on_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model_on_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model_on_dataset.metricValue = $metricValue // 有的含有%之类的，所以设为string类型

// Model - MODEL_INTRODUCED_IN_PAPER -> Paper
MATCH (model:Model {name: $modelName})
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.paperUrl = $paperUrl
SET paper.paperDate = $paperDate
MERGE (model)-[model_in_paper:MODEL_INTRODUCED_IN_PAPER]->(paper)
  ON CREATE SET model_in_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model_in_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Model - MODEL_IMPLEMENTED_BY_REPO -> Repo
MATCH (model:Model {name: $modelName})
MATCH (repo:Repository)
  WHERE repo.nameWithOwner =~ '(?i)' + $nameWithOwner
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.githubUrl = $githubUrl
MERGE (model)-[implements:MODEL_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET implements.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET implements.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Task - TASK_HAS_SUBTASK -> Subtask
// 第一种思路：判断如果来真正的Task的时候这么做
/*MATCH (task:Task {name: $taskName})
MERGE (subtask:Subtask {name: $subtaskName})
  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.description = $description
MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)
  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
// 第二种思路：只保留Task
MATCH (task:Task {name: $taskName})
MERGE (subtask:Task {name: $subtaskName})
  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.description = $description
MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)
  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')





// 2 links-between-papers-and-code.json

// Paper （links-between-papers-and-code.json）
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.paperswithcodeUrl = $paperswithcodeUrl
SET paper.paperArxivId = $paperArxivId
SET paper.paperUrlAbs = $paperUrlAbs
SET paper.paperUrlPdf = $paperUrlPdf
// Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
// 这里大坑，url截取的可能有大小写错误导致match不到，可以不区分大小写
MATCH (paper:Paper {paperTitle: $paperTitle})
MATCH (repo:Repository)
  WHERE repo.nameWithOwner =~ '(?i)' + $nameWithOwner
MERGE (paper)-[link:PAPER_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET link.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET link.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET link.mentionedInPaper = $mentionedInPaper
SET link.mentionedInGithub = $mentionedInGithub
SET link.framework = $framework


// 3 Methods.json

// Method （methods.json）
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.paperswithcodeUrl = $paperswithcodeUrl
SET method.fullName = $fullName
SET method.description = $description
SET method.codeSnippetUrl = $codeSnippetUrl
SET method.introducedYear = $introducedYear
// Method - METHOD_INTRODUCED_IN_PAPER -> Paper
/*MATCH (method:Method {name: $methodName})
MERGE (new_paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET new_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(new_paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
// 这个Paper是introPaper，title用连字符表示的，分词处理
// match paper start with
MATCH (paper:Paper)
  WHERE paper.paperTitle =~ '(?i)$firstToken.*' // 需要在程序中字符串拼接
RETURN paper.paperTitle
// if paper exist
MATCH (method:Method {name: $methodName})
MERGE (paper:Paper {paperTitle: $existPaperTitle})
SET paper.dashPaperTitle = $dashPaperTitle
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// if paper not exist
MATCH (method:Method {name: $methodName})
CREATE (new_paper:Paper) // 这里不能用MERGE不然就查到所有
SET new_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.dashPaperTitle = $dashPaperTitle
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(new_paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

/**/


// Collection - COLLECTION_HAS_METHOD -> Method
MATCH (method:Method {name: $methodName})
MERGE (coll:Collection {name: $collectionName})
  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (coll)-[has:COLLECTION_HAS_METHOD]->(method)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// Area - AREA_HAS_COLLECTION -> Collection
MATCH (coll:Collection {name: $collectionName})
MERGE (area:Area {area: $areaName})
  ON CREATE SET area.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET area.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET area.areaId = $areaId
MERGE (area)-[has:AREA_HAS_COLLECTION]->(coll)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')






// 4 papers-with-abstracts.json

// Paper （papers-with-abstracts.json）
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.paperswithcodeUrl = $paperswithcodeUrl
SET paper.arxivId = $arxivId
SET paper.abstract = $abstract
SET paper.urlAbs = $urlAbs
SET paper.urlPdf = $urlPdf
SET paper.proceeding = $proceeding
SET paper.date = $date

// Paper - PAPER_WRITTEN_BY_AUTHOR -> Author
/*MATCH (paper:Paper {paperTitle: $paperTitle})
MERGE (author:Author {name: $authorName})
  ON CREATE SET author.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET author.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper)-[written:PAPER_WRITTEN_BY_AUTHOR]->(author)
  ON CREATE SET written.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET written.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/

// Task - TASK_HAS_PAPER -> Paper
MATCH (paper:Paper {paperTitle: $paperTitle})
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task)-[has:TASK_HAS_PAPER]->(paper)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Method （papers-with-abstracts.json）
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.fullName = $fullName
SET method.description = $description
SET method.codeSnippetUrl = $codeSnippetUrl
SET method.introducedYear = $introducedYear

// Paper - PAPER_USES_METHOD -> Method
MATCH (paper:Paper {paperTitle: $paperTitle})
MATCH (method:Method {name: $methodName})
MERGE (paper)-[use:PAPER_USES_METHOD]-(method)
  ON CREATE SET use.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET use.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Method - METHOD_MAIN_UNDER_COLLECTION -> Collection
MATCH (method:Method {name: $methodName})
MERGE (coll:Collection {name: $collectionName})
  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.description = $collectionDescription
MERGE (method)-[main:METHOD_MAIN_UNDER_COLLECTION]->(coll)
  ON CREATE SET main.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET main.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// Area - AREA_HAS_COLLECTION -> Collection 同 methods.json
