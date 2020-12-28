from neo4j import GraphDatabase
import logging
from neo4j.exceptions import ServiceUnavailable
import pandas as pd


class CategoryApp(object):

    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def close(self):
        self.driver.close()

    def get_category_and_count(self):
        with self.driver.session() as session:
            categoryName_list = session.read_transaction(self._get_categoryNames)
            taskCount_list = session.read_transaction(self._get_categoryTaskCount)
            paperCount_list = session.read_transaction(self._get_categoryPaperCount)
            modelCount_list = session.read_transaction(self._get_categoryModelCount)
            repoCount_list = session.read_transaction(self._get_categoryRepoCount)
        data = {
            "categoryName": categoryName_list,
            "taskCount": taskCount_list,
            "paperCount": paperCount_list,
            "modelCount": modelCount_list,
            "repositoryCount": repoCount_list
                }
        # 写入文件
        df = pd.DataFrame(data=data)
        df.to_csv(r'C:\Disk_Dev\Repository\github-KG\github-KG-python\result\category_and_count.csv', index=False)
        return data

    @staticmethod
    def _get_categoryRepoCount(tx):
        query = """
MATCH (cate:Category)
MATCH (cate)-[:CATEGORY_HAS_TASK]->(:Task)-[:TASK_HAS_PAPER]->(:Paper)-[:PAPER_IMPLEMENTED_BY_REPO]->(repo:Repository)
RETURN cate.name AS categoryName,
       count(DISTINCT repo) AS repoCount
  ORDER BY categoryName ASC
            """
        result = tx.run(query)
        row_list = [row["repoCount"] for row in result]
        return row_list

    @staticmethod
    def _get_categoryModelCount(tx):
        query = """
MATCH (cate:Category)
MATCH (cate)-[:CATEGORY_HAS_TASK]->(task:Task)-[:TASK_HAS_MODEL]->(model:Model)
RETURN cate.name AS categoryName,
       count(DISTINCT model) AS modelCount // 有些 model 属于多个 task
  ORDER BY categoryName ASC
            """
        result = tx.run(query)
        row_list = [row["modelCount"] for row in result]
        return row_list

    @staticmethod
    def _get_categoryPaperCount(tx):
        query = """
        MATCH (cate:Category)
MATCH (cate)-[:CATEGORY_HAS_TASK]->(task:Task)-[:TASK_HAS_PAPER]->(paper:Paper)
RETURN cate.name AS categoryName,
       count(DISTINCT paper) AS paperCount // 有些paper属于多个task，对于category聚合而言，要去重
  ORDER BY categoryName ASC
            """
        result = tx.run(query)
        row_list = [row["paperCount"] for row in result]
        return row_list

    @staticmethod
    def _get_categoryTaskCount(tx):
        query = """
        MATCH (cate:Category)-[:CATEGORY_HAS_TASK]->(task:Task)
            RETURN cate.name AS categoryName, // 相当于 GROUP BY category.name
            count(DISTINCT task) AS taskCount // 有些task属于多个Category，计算重复
            ORDER BY categoryName ASC
            """
        result = tx.run(query)
        row_list = [row["taskCount"] for row in result]
        return row_list

    @staticmethod
    def _get_categoryNames(tx):
        query = """
MATCH (cate:Category)
RETURN cate.name AS categoryName
ORDER BY categoryName
            """
        result = tx.run(query)
        row_list = [row["categoryName"] for row in result]
        return row_list


class App:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def close(self):

        # Don't forget to close the driver connection when you are finished with it
        self.driver.close()

    def create_friendship(self, person1_name, person2_name):
        with self.driver.session() as session:
            # Write transactions allow the driver to handle retries and transient errors
            result = session.write_transaction(
                self._create_and_return_friendship, person1_name, person2_name)
            for row in result:
                print("Created friendship between: {p1}, {p2}".format(p1=row['p1'], p2=row['p2']))

    @staticmethod
    def _create_and_return_friendship(tx, person1_name, person2_name):

        # To learn more about the Cypher syntax, see https://neo4j.com/docs/cypher-manual/current/
        # The Reference Card is also a good resource for keywords https://neo4j.com/docs/cypherrefcard/current/
        query = (
            "CREATE (p1:Person { name: $person1_name }) "
            "CREATE (p2:Person { name: $person2_name }) "
            "CREATE (p1)-[:KNOWS]->(p2) "
            "RETURN p1, p2"
        )
        result = tx.run(query, person1_name=person1_name, person2_name=person2_name)
        try:
            return [{"p1": row["p1"]["name"], "p2": row["p2"]["name"]}
                    for row in result]
        # Capture any errors along with the query and data for traceability
        except ServiceUnavailable as exception:
            logging.error("{query} raised an error: \n {exception}".format(
                query=query, exception=exception))
        raise

    def find_person(self, person_name):
        with self.driver.session() as session:
            result = session.read_transaction(self._find_and_return_person, person_name)
        for row in result:
            print("Found person: {row}".format(row=row))

    @staticmethod
    def _find_and_return_person(tx, person_name):
        query = (
            "MATCH (p:Person) "
            "WHERE p.name = $person_name "
            "RETURN p.name AS name"
        )

        result = tx.run(query, person_name=person_name)
        return [row["name"] for row in result]

