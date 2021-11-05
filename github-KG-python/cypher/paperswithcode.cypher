// 1 evaluation-tables.json

// Task - TASK_UNDER_CATEGORY -> Category
// 好像还不能这么一步到位，因为无法保证不重复
//MERGE (task:Task {name: $task})-[under:UNDER_CATEGORY]-(category:CATEGORY {name: $category})

// 【实体】 Task
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.description = $description

// 【关系】 Category - CATEGORY_HAS_TASK -> Task
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (category:Category {name: $category})
  ON CREATE SET category.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET category.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (category)-[has:CATEGORY_HAS_TASK]->(task)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Task - TASK_HAS_DATASET -> Dataset
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (dataset:Dataset {name: $datasetName})
  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dataset.description = $description
MERGE (task)-[has_dataset:TASK_HAS_DATASET]->(dataset)
  ON CREATE SET has_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// todo 这里 Model 和 Paper 相当于平级，2020年只是以 Model 为中心建立了关系，
// 所以还可以加上的关系有一下，这些在其他json数据中有重叠，但仅依靠其他可能有遗漏
// Task - TASK_HAS_PAPER -> Paper
// Paper - PAPER_ON_DATASET -> Dataset
// Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo

// 【关系】 Task - TASK_HAS_MODEL -> Model
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (model:Model {name: $modelName})
  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task)-[task_has_model:TASK_HAS_MODEL]->(model)
  ON CREATE SET task_has_model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task_has_model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Model - MODEL_ON_DATASET -> Dataset
MERGE (dataset:Dataset {name: $datasetName})
  ON CREATE SET dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (model:Model {name: $modelName})
  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// 把metric作为 Dataset & Model之间属性 最后有几个metric就插入几条关系
MERGE (model)-[model_on_dataset:MODEL_ON_DATASET {metricName: $metricName}]->(dataset)
  ON CREATE SET model_on_dataset.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model_on_dataset.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// 有的含有%之类的，所以设为string类型
SET model_on_dataset.metricValue = $metricValue

// 【实体】 Paper
// 【关系】 Model - MODEL_INTRODUCED_IN_PAPER -> Paper
MERGE (model:Model {name: $modelName})
  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.paperUrl = $paperUrl
SET paper.paperDate = $paperDate
MERGE (model)-[model_in_paper:MODEL_INTRODUCED_IN_PAPER]->(paper)
  ON CREATE SET model_in_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model_in_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Model - MODEL_IMPLEMENTED_BY_REPO -> Repo
/*MERGE (model:Model {name: $modelName})
// 这里的思考是不区分大小写，以免因为大小写问题链接不到repo，但其实也大可不必
MATCH (repo:Repository)
  WHERE repo.nameWithOwner =~ '(?i)' + $nameWithOwner
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.githubUrl = $githubUrl
MERGE (model)-[implements:MODEL_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET implements.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET implements.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
MERGE (model:Model {name: $modelName})
  ON CREATE SET model.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET model.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (repo:Repository {nameWithOwner: $nameWithOwner})
  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.githubUrl = $githubUrl
MERGE (model)-[implements:MODEL_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET implements.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET implements.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【+关系】 Task - TASK_HAS_PAPER -> Paper
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task)-[predicate:TASK_HAS_PAPER]->(paper)
  ON CREATE SET predicate.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET predicate.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【+关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// 只要repo 能对齐，先导入 pwc 还是 先导入 github 无所谓，怕就怕不能对齐
MERGE (repo:Repository {nameWithOwner: $nameWithOwner})
  ON CREATE SET repo.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET repo.githubUrl = $githubUrl
MERGE (paper)-[predicate:PAPER_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET predicate.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET predicate.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Task - TASK_HAS_SUBTASK -> Subtask
// 第一种思路：判断如果来Task还是subTask
/*MATCH (task:Task {name: $taskName})
MERGE (subtask:Subtask {name: $subtaskName})
  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.description = $description
MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)
  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
// 第二种思路：把 subTask 也看作 Task
// 【关系】 Task - TASK_HAS_SUBTASK -> Subtask
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (subtask:Task {name: $subtaskName})
  ON CREATE SET subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET subtask.description = $description
MERGE (task)-[has_subtask:TASK_HAS_SUBTASK]->(subtask)
  ON CREATE SET has_subtask.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has_subtask.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')



// 2 links-between-papers-and-code.json

// 【实体】Paper （links-between-papers-and-code.json）
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.paperswithcodeUrl = $paperswithcodeUrl
SET paper.paperArxivId = $paperArxivId
SET paper.paperUrlAbs = $paperUrlAbs
SET paper.paperUrlPdf = $paperUrlPdf

// 【关系】 Paper - PAPER_IMPLEMENTED_BY_REPO -> Repo
// todo 这里有坑，url截取的可能有大小写错误导致match不到
// 可以不区分大小写，所以这里想match repo的话就必须先导入 github
// 这种情况merge repo也不好因为怕大小写不对，后面github merge 不到，只能match
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
WITH paper
MATCH (repo:Repository)
  WHERE repo.nameWithOwner =~ '(?i)' + $nameWithOwner
MERGE (paper)-[link:PAPER_IMPLEMENTED_BY_REPO]->(repo)
  ON CREATE SET link.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET link.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET link.mentionedInPaper = $mentionedInPaper
SET link.mentionedInGithub = $mentionedInGithub
SET link.framework = $framework


// 3 Methods.json

// 【实体】 Method （methods.json）
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.paperswithcodeUrl = $paperswithcodeUrl
SET method.fullName = $fullName
SET method.description = $description
SET method.codeSnippetUrl = $codeSnippetUrl
SET method.introducedYear = $introducedYear

// 【关系】 Method - METHOD_INTRODUCED_IN_PAPER -> Paper （注意，不同于 PAPER_USES_METHOD）
/*MATCH (method:Method {name: $methodName})
MERGE (new_paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET new_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(new_paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/
// todo 这个Paper是introPaper，title用连字符表示的，分词处理
// match paper start with
MATCH (paper:Paper)
  WHERE paper.paperTitle =~ '(?i)$firstToken.*' // 需要在程序中字符串拼接
RETURN paper.paperTitle
// if paper exist
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper:Paper {paperTitle: $existPaperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.dashPaperTitle = $dashPaperTitle
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
// if paper not exist
/*MATCH (method:Method {name: $methodName})
CREATE (new_paper:Paper) // 这里不能用MERGE不然就查到所有
SET new_paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET new_paper.dashPaperTitle = $dashPaperTitle
MERGE (method)-[intro:METHOD_INTRODUCED_IN_PAPER]->(new_paper)
  ON CREATE SET intro.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET intro.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/

// 【关系】 Collection - COLLECTION_HAS_METHOD -> Method
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (coll:Collection {name: $collectionName})
  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (coll)-[has:COLLECTION_HAS_METHOD]->(method)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Area - AREA_HAS_COLLECTION -> Collection
MERGE (coll:Collection {name: $collectionName})
  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (area:Area {area: $areaName})
  ON CREATE SET area.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET area.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET area.areaId = $areaId
MERGE (area)-[has:AREA_HAS_COLLECTION]->(coll)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')


// 4 papers-with-abstracts.json

// 【实体】 Paper （papers-with-abstracts.json）
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

// 【关系】 PAPER_WRITTEN_BY_AUTHOR -> Author
// todo 实体对齐问题 Author的同名如果放任merge的话，是一个比较严重的知识错误，因为简写同名的概率还是挺大的
/*MATCH (paper:Paper {paperTitle: $paperTitle})
MERGE (author:Author {name: $authorName})
  ON CREATE SET author.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET author.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper)-[written:PAPER_WRITTEN_BY_AUTHOR]->(author)
  ON CREATE SET written.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET written.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')*/

// 【关系】【重叠1】Task - TASK_HAS_PAPER -> Paper
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task:Task {name: $taskName})
  ON CREATE SET task.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET task.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (task)-[has:TASK_HAS_PAPER]->(paper)
  ON CREATE SET has.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET has.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【实体】【重叠3】Method （papers-with-abstracts.json）
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.fullName = $fullName
SET method.description = $description
SET method.codeSnippetUrl = $codeSnippetUrl
SET method.introducedYear = $introducedYear

// 【关系】 Paper - PAPER_USES_METHOD -> Method
MERGE (paper:Paper {paperTitle: $paperTitle})
  ON CREATE SET paper.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET paper.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (paper)-[use:PAPER_USES_METHOD]-(method)
  ON CREATE SET use.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET use.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

// 【关系】 Method - METHOD_MAIN_UNDER_COLLECTION -> Collection
MERGE (method:Method {name: $methodName})
  ON CREATE SET method.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET method.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
MERGE (coll:Collection {name: $collectionName})
  ON CREATE SET coll.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET coll.description = $collectionDescription
MERGE (method)-[main:METHOD_MAIN_UNDER_COLLECTION]->(coll)
  ON CREATE SET main.gmtCreate = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')
SET main.gmtModified = apoc.date.format(timestamp(), 'ms', 'yyyy-MM-dd HH:mm:ss', 'CTT')

