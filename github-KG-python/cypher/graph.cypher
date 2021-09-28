// 根据主键找出entity
// package
UNWIND $entityMapList AS map
MATCH (entity_i:Package {nameWithManager: map.key})
RETURN entity_i.nameWithManager AS key, entity_i.repoIDF AS repoIDF
// language
UNWIND $entityMapList AS map
MATCH (entity_i:Language {name: map.key})
RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF
// task
UNWIND $entityMapList AS map
MATCH (entity_i:Task {name: map.key})
RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF
// method
UNWIND $entityMapList AS map
MATCH (entity_i:Method {name: map.key})
RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF
// dataset
UNWIND $entityMapList AS map
MATCH (entity_i:Dataset {name: map.key})
RETURN entity_i.name AS key, entity_i.repoIDF AS repoIDF


// 实体探索
// 同样可以使用各种方法，既可以UCF也可以ICF，依据和repo的关联关系
// ICF_cosine_tfidf
// ICF_cosine_tfidf_package
UNWIND $targetMapList AS map
MATCH (tar_i:Package {nameWithManager: map.key})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(:Repository)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(tar_j:Package)
  WHERE NOT tar_j.nameWithManager IN $targetKeyList
RETURN tar_j.nameWithManager AS key, sum(1.0 * r_i.packageTfIdf * r_j.packageTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_tfidf_language
UNWIND $targetMapList AS map
MATCH (tar_i:Language {name: map.key})<-[r_i:REPO_USES_LANGUAGE]-(:Repository)-[r_j:REPO_USES_LANGUAGE]->(tar_j:Language)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 * r_i.languageTfIdf * r_j.languageTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_tfidf_task
UNWIND $targetMapList AS map
MATCH (tar_i:Task {name: map.key})<-[r_i:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(:Repository)-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(tar_j:Task)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 * r_i.taskTfIdf * r_j.taskTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_tfidf_method
UNWIND $targetMapList AS map
MATCH (tar_i:Method {name: map.key})<-[r_i:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(:Repository)-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(tar_j:Method)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 * r_i.methodTfIdf * r_j.methodTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_tfidf_dataset
UNWIND $targetMapList AS map
MATCH (tar_i:Dataset {name: map.key})<-[r_i:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(:Repository)-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(tar_j:Dataset)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 * r_i.datasetTfIdf * r_j.datasetTfIdf / sqrt(tar_i.repoTfIdfQuadraticSum * tar_j.repoTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN

// ICF_cosine
// ICF_cosine_package
UNWIND $targetMapList AS map
MATCH (tar_i:Package {nameWithManager: map.key})<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository)-[:REPO_DEPENDS_ON_PACKAGE]->(tar_j:Package)
  WHERE NOT tar_j.nameWithManager IN $targetKeyList
RETURN tar_j.nameWithManager AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_language
UNWIND $targetMapList AS map
MATCH (tar_i:Language {name: map.key})<-[:REPO_USES_LANGUAGE]-(:Repository)-[:REPO_USES_LANGUAGE]->(tar_j:Language)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_task
UNWIND $targetMapList AS map
MATCH (tar_i:Task {name: map.key})<-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(:Repository)-[:REPO_IMPLEMENTS_PAPER_UNDER_TASK]->(tar_j:Task)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_method
UNWIND $targetMapList AS map
MATCH (tar_i:Method {name: map.key})<-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(:Repository)-[:REPO_IMPLEMENTS_PAPER_USES_METHOD]->(tar_j:Method)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN
// ICF_cosine_dataset
UNWIND $targetMapList AS map
MATCH (tar_i:Dataset {name: map.key})<-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(:Repository)-[:REPO_IMPLEMENTS_MODEL_ON_DATASET]->(tar_j:Dataset)
  WHERE NOT tar_j.name IN $targetKeyList
RETURN tar_j.name AS key, sum(1.0 / sqrt(tar_i.repoDegree * tar_j.repoDegree)) AS score
  ORDER BY score DESC, key DESC
  LIMIT $topN


// Graph
// todo （似乎不能多个UNWIND叠加，只好分开，在程序中汇总）
// UCF 非图中用户写法

// UCF_cosine_tfidf
// UCF_cosine_tfidf_package
UNWIND $entityMapList AS map
MATCH (package_i:Package {nameWithManager: map.key})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
/*  WHERE exists {
    MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]-(package_j:Package)
      WHERE package_j <> package_i
    }*/
// 可以用的权重 1、单1.0； 2、repoIDF 3、TfIdf
WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.packageTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
// 可以用的权重 1、单1.0； 2、entityIDF 3、inverseTfIdf
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree
// UCF_cosine_tfidf_Language
UNWIND $entityMapList AS map
MATCH (:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.languageTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree
// UCF_cosine_tfidf_Task
UNWIND $entityMapList AS map
MATCH (:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)
WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.taskTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree
// UCF_cosine_tfidf_Method
UNWIND $entityMapList AS map
MATCH (:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)
WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.methodTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree
// UCF_cosine_tfidf_Dataset
UNWIND $entityMapList AS map
MATCH (:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)
WITH repo_j, sum(1.0 * map.TfIdf * r_j.TfIdf / sqrt($entityTfIdfQuadraticSum * repo_j.datasetTfIdfQuadraticSum)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score, package_j.repoDegree AS repoDegree

// UCF_PathSim
// 如果2跳压缩为1跳后仍然把路径条数视为 1 ，那分母的平方和其实就可以用度来代替，如果严格按照路径条数，则只能老实用条数的平方和
// UCF_PathSim_Pac
UNWIND $entityMapList AS map
MATCH (:Package {nameWithManager: map.key})<-[r_j:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.packageDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
// UCF_PathSim_La
UNWIND $entityMapList AS map
MATCH (:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)
WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.languageDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
// UCF_PathSim_Ta
UNWIND $entityMapList AS map
MATCH (:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)
WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.taskDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
// UCF_PathSim_Me
UNWIND $entityMapList AS map
MATCH (:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)
WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.methodDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
// UCF_PathSim_Da
UNWIND $entityMapList AS map
MATCH (:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)
WITH repo_j, sum(2 * 1.0 / (size($entityMapList) + repo_j.datasetDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $packageKeyList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score

// UCF_cosine
// UCF_cosine_Pac
// 找的KNN如果全是依赖完全相同的（余弦相似度为1.0），结果会最后被全部过滤，所以应该找那些【不只依赖于】训练集的repo_j
UNWIND $targetMapList AS map
MATCH (package_i:Package {nameWithManager: map.key})<-[r_i:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
/*  WHERE exists {

MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]-(package_j:Package)
  WHERE package_j <> package_i
}*/
WITH repo_j, sum(1.0 / (sqrt(size($targetMapList) * repo_j.packageDegree))) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[r_j:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
/*  ORDER BY score DESC, recommend DESC
  LIMIT $topN*/
// UCF_cosine_Language
UNWIND $targetMapList AS map
MATCH (language:Language {name: map.key})<-[r_j:REPO_USES_LANGUAGE]-(repo_j:Repository)
WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.languageDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
/*  ORDER BY score DESC, recommend DESC
  LIMIT $topN*/
// UCF_cosine_Task
UNWIND $targetMapList AS map
MATCH (task:Task {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_UNDER_TASK]-(repo_j:Repository)
WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.taskDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
/*  ORDER BY score DESC, recommend DESC
  LIMIT $topN*/
// UCF_cosine_Method
UNWIND $targetMapList AS map
MATCH (method:Method {name: map.key})<-[r_j:REPO_IMPLEMENTS_PAPER_USES_METHOD]-(repo_j:Repository)
WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.methodDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
/*  ORDER BY score DESC, recommend DESC
  LIMIT $topN*/
// UCF_cosine_Dataset
UNWIND $targetMapList AS map
MATCH (dataset:Dataset {name: map.key})<-[r_j:REPO_IMPLEMENTS_MODEL_ON_DATASET]-(repo_j:Repository)
WITH repo_j, sum(1.0 / sqrt(size($targetMapList) * repo_j.datasetDegree)) AS score
  ORDER BY score DESC, repo_j.nameWithOwner DESC
  LIMIT $UCF_KNN
MATCH (repo_j)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend, sum(1.0 * score) AS score
/*
  ORDER BY score DESC, recommend DESC
  LIMIT $topN
*/


// ICF_cosine_Pac
// 图中用户写法
MATCH(repo_i:Repository {nameWithOwner: $nameWithOwner})-[:REPO_DEPENDS_ON_PACKAGE]->(package_i:Package)
       <-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)-[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1.0 / sqrt(package_i.repoDegree * package_j.repoDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// ICF_PathSim_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum(1.0 * 2 / (package_i.repoDegree + package_j.repoDegree)) AS score
  ORDER BY score DESC
  LIMIT $topN
// ICF_IUF_cosine_Pac
UNWIND $dependencyMapList AS map
MATCH (package_i:Package {nameWithManager: map.nameWithManager})<-[:REPO_DEPENDS_ON_PACKAGE]-(repo_j:Repository)
        -[:REPO_DEPENDS_ON_PACKAGE]->(package_j:Package)
  WHERE NOT package_j.nameWithManager IN $dependencyNameList
RETURN package_j.nameWithManager AS recommend,
       sum((1.0 * (1 / log(1 + repo_j.packageDegree))) / sqrt(package_i.repoDegree * package_j.repoDegree))
       AS score
  ORDER BY score DESC
  LIMIT $topN


// Popular 热门推荐
MATCH (package:Package)<-[:REPO_DEPENDS_ON_PACKAGE]-(repo:Repository)
  WHERE NOT package.nameWithManager IN $dependencyNameList
WITH package.nameWithManager AS nameWithManager, count(repo) AS repoDegree
RETURN nameWithManager AS recommend, repoDegree AS score
  ORDER BY repoDegree DESC
  LIMIT $topN

// 随机推荐
MATCH (package:Package)
  WHERE exists((package)<-[:REPO_DEPENDS_ON_PACKAGE]-(:Repository))
  AND NOT package.nameWithManager IN $dependencyNameList
RETURN DISTINCT package.nameWithManager AS recommend, rand() AS score
  ORDER BY score
  LIMIT $topN