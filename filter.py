import re

class Pair:
	def __init__(self, name, views):
		self.name = name
		self.views = views

# open the file
file = open("pagecounts-20140701-000000","r")
print file.name

title = ['Media:','Special:','Talk:','User:','User_talk:','Project:','Project_talk:','File:','File_talk:',
		'MediaWiki:','MediaWiki_talk:','Template:','Template_talk:','Help:','Help_talk:','Category:',
		'Category_talk:','Portal:','Wikipedia:','Wikipedia_talk:']
extension = ['.jpg','.gif','.png','.JPG','.GIF','.PNG','.txt','.ico']
boilerplate = ['404_error/','Main_Page','Hypertext_Transfer_Protocol','Favicon.ico','Search']

# <project name> <page title> <number of accesses> <total data returned in bytes> 
pattern = re.compile('(.*\s)(.*\s)(.*\s)(.*)')
list = []

while True:
	line = file.readline()
	if len(line) == 0:
		break
	m = pattern.match(line)

	if m:
		project_name = m.group(1).strip()
		page_title = m.group(2).strip()
		accesses = m.group(3).strip()
		returned_bytes = m.group(4).strip()
		# rule 1
		if(project_name == 'en'):
			pattern_ = re.compile('(.*\:)(.*)')
			m_ = pattern_.match(page_title)

			if m_:
				prefix = m_.group(1).strip()
				title = m_.group(2).strip()
				# rule 2
				if (prefix in title):
					continue
				#rule 3
				if (title[0].islower()):
					continue
				#rule 4
				img = page_title[-4:]
				if (img in extension):
					continue
				#rule 5
				if (page_title in boilerplate):
					continue
			else:
				continue
			# put line into a list after filter
			pair = Pair(page_title,accesses)
			list.append(pair)
		else:
			continue
	else:
		continue
else:
	print 'critical failure...'

list.sort()

i = 0
for element in list:
	print '#',i,element.name,element.views
	i += 1 
	
# close the file
file.close