package de.marmaro.krt.ffupdater.github;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Awesome Pojo Generator
 * Represents JSON objects like https://api.github.com/repos/mozilla-mobile/focus-android/releases/11799446
 * See https://developer.github.com/v3/repos/releases/
 */
public class Release implements Serializable{
    @SerializedName("tag_name")
    @Expose
    private String tagName;
    @SerializedName("author")
    @Expose
    private User author;
    @SerializedName("created_at")
    @Expose
    private String createdAt;
    @SerializedName("body")
    @Expose
    private String body;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("assets_url")
    @Expose
    private String assetsUrl;
    @SerializedName("assets")
    @Expose
    private List<Asset> assets;
    @SerializedName("prerelease")
    @Expose
    private Boolean prerelease;
    @SerializedName("html_url")
    @Expose
    private String htmlUrl;
    @SerializedName("target_commitish")
    @Expose
    private String targetCommitish;
    @SerializedName("draft")
    @Expose
    private Boolean draft;
    @SerializedName("zipball_url")
    @Expose
    private String zipballUrl;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("upload_url")
    @Expose
    private String UploadUrl;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("published_at")
    @Expose
    private String publishedAt;
    @SerializedName("tarball_url")
    @Expose
    private String tarballUrl;
    @SerializedName("node_id")
    @Expose
    private String nodeId;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAssetsUrl() {
        return assetsUrl;
    }

    public void setAssetsUrl(String assetsUrl) {
        this.assetsUrl = assetsUrl;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public Boolean getPrerelease() {
        return prerelease;
    }

    public void setPrerelease(Boolean prerelease) {
        this.prerelease = prerelease;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getTargetCommitish() {
        return targetCommitish;
    }

    public void setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    public String getZipballUrl() {
        return zipballUrl;
    }

    public void setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUploadUrl() {
        return UploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        UploadUrl = uploadUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getTarballUrl() {
        return tarballUrl;
    }

    public void setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}