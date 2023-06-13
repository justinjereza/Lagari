PROJECT = Lagari
VERSION = 1.0.3
PROJECT_ROOT = /root/$(PROJECT)
JAR = $(PROJECT)-$(VERSION).jar
SNAPSHOT_JAR = target/$(PROJECT)-$(VERSION)-SNAPSHOT.jar

PODMAN ?= podman
IMAGE ?= localhost/openjdk17:alpine

M2_ROOT = /root/.m2
MVN = sh $(SPIGOTMC_ROOT)/apache-maven-3.6.0/bin/mvn

SPIGOTMC_VERSION ?= 1.20
SPIGOTMC_ROOT = $(PROJECT_ROOT)/spigotmc
SPIGOTMC_JAR = $(SPIGOTMC_ROOT)/spigot-$(SPIGOTMC_VERSION).jar
SPIGOTMC_API_JAR = $(SPIGOTMC_ROOT)/Spigot/Spigot-API/target/spigot-api-$(SPIGOTMC_VERSION)-R0.1-SNAPSHOT-shaded.jar
BUILDTOOLS_URL = https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar

SPIGOTMC_NAME ?= spigotmc
SPIGOTMC_DATA ?= spigotmc-data

PODMAN_OPTIONS = --rm --security-opt label=disable --volume "$(CURDIR):$(PROJECT_ROOT)"

.PHONY: all run clean distclean image

all: $(JAR)

$(JAR): | spigotmc
	mkdir -p .m2
	$(PODMAN) run $(PODMAN_OPTIONS) --volume "$(CURDIR)/.m2:$(M2_ROOT)" --workdir $(PROJECT_ROOT) $(IMAGE) \
		$(MVN) install:install-file -Dfile=$(SPIGOTMC_API_JAR)
	$(PODMAN) run $(PODMAN_OPTIONS) --volume "$(CURDIR)/.m2:$(M2_ROOT)" --workdir $(PROJECT_ROOT) $(IMAGE) $(MVN) verify
	mv $(SNAPSHOT_JAR) $@

run: $(JAR)
	mkdir -p $(SPIGOTMC_DATA)/plugins
	[ -f $(SPIGOTMC_DATA)/eula.txt ] || echo "eula=true" > $(SPIGOTMC_DATA)/eula.txt
	[ -f $(SPIGOTMC_DATA)/plugins/$(JAR) ] || cp $(JAR) $(SPIGOTMC_DATA)/plugins
	$(PODMAN) run $(PODMAN_OPTIONS) --detach --interactive --name $(SPIGOTMC_NAME) --publish 25565:25565 --tty \
		--volume "$(CURDIR)/$(SPIGOTMC_DATA):$(PROJECT_ROOT)/$(SPIGOTMC_DATA)" --workdir $(PROJECT_ROOT)/$(SPIGOTMC_DATA) $(IMAGE) \
		java -jar $(SPIGOTMC_JAR)

doc: | spigotmc
	mkdir -p $@
	$(PODMAN) run $(PODMAN_OPTIONS) --workdir $(PROJECT_ROOT)/$@ $(IMAGE) \
		javadoc -classpath $(SPIGOTMC_API_JAR) -sourcepath $(PROJECT_ROOT)/src/main/java com.github.justinjereza.Lagari

clean:
	-$(PODMAN) image exists $(IMAGE) && $(PODMAN) run $(PODMAN_OPTIONS) --workdir $(PROJECT_ROOT) $(IMAGE) $(MVN) clean
	$(RM) -r $(JAR) $(SPIGOTMC_DATA)/plugins/$(JAR)

distclean: clean
	$(RM) -r .m2 doc spigotmc
	$(PODMAN) image rm $(IMAGE)

image:
	-$(PODMAN) image exists $(IMAGE) || $(PODMAN) build --tag $(IMAGE) $(CURDIR)

spigotmc: image
	mkdir -p $@
	$(PODMAN) run $(PODMAN_OPTIONS) --workdir $(SPIGOTMC_ROOT) $(IMAGE) \
		sh -c "[ -f BuildTools.jar ] || wget $(BUILDTOOLS_URL)"
	$(PODMAN) run $(PODMAN_OPTIONS) --workdir $(SPIGOTMC_ROOT) $(IMAGE) \
		sh -c "[ -f $(SPIGOTMC_JAR) ] || java -jar BuildTools.jar -rev $(SPIGOTMC_VERSION)"
